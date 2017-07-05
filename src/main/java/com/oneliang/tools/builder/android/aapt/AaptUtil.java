package com.oneliang.tools.builder.android.aapt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.oneliang.Constant;
import com.oneliang.tools.builder.android.aapt.RDotTxtEntry.IdType;
import com.oneliang.tools.builder.android.aapt.RDotTxtEntry.RType;
import com.oneliang.tools.builder.android.patch.PatchUtil.PatchUtilException;
import com.oneliang.util.common.JavaXmlUtil;
import com.oneliang.util.common.StringUtil;
import com.oneliang.util.file.FileUtil;

public final class AaptUtil {

    private static final String ID_DEFINITION_PREFIX = "@+id/";
    private static final String ITEM_TAG = "item";

    private static final XPathExpression ANDROID_ID_USAGE = createExpression("//@*[starts-with(., '@') and " + "not(starts-with(., '@+')) and " + "not(starts-with(., '@android:')) and " + "not(starts-with(., '@null'))]");

    private static final XPathExpression ANDROID_ID_DEFINITION = createExpression("//@*[starts-with(., '@+') and " + "not(starts-with(., '@+android:id'))]");

    private static final Map<String, RType> RESOURCE_TYPES = getResourceTypes();
    private static final List<String> IGNORED_TAGS = Arrays.asList("eat-comment", "skip");

    private static XPathExpression createExpression(String expressionStr) {
        try {
            return XPathFactory.newInstance().newXPath().compile(expressionStr);
        } catch (XPathExpressionException e) {
            throw new AaptUtilException(e);
        }
    }

    private static Map<String, RType> getResourceTypes() {
        Map<String, RType> types = new HashMap<String, RType>();
        for (RType rType : RType.values()) {
            types.put(rType.toString(), rType);
        }
        types.put("string-array", RType.ARRAY);
        types.put("integer-array", RType.ARRAY);
        types.put("declare-styleable", RType.STYLEABLE);
        return types;
    }

    public static AaptResourceCollector collectResource(List<String> resourceDirectoryList) {
        return collectResource(resourceDirectoryList, null);
    }

    public static AaptResourceCollector collectResource(List<String> resourceDirectoryList, Map<RType, Set<RDotTxtEntry>> rTypeResourceMap) {
        AaptResourceCollector resourceCollector = new AaptResourceCollector(rTypeResourceMap);
        List<RDotTxtEntry> references = new ArrayList<RDotTxtEntry>();
        for (String resourceDirectory : resourceDirectoryList) {
            try {
                collectResources(resourceDirectory, resourceCollector);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        for (String resourceDirectory : resourceDirectoryList) {
            try {
                processXmlFilesForIds(resourceDirectory, references, resourceCollector);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return resourceCollector;
    }

    public static void processXmlFilesForIds(String resourceDirectory, List<RDotTxtEntry> references, AaptResourceCollector resourceCollector) throws Exception {
        FileUtil.MatchOption matchOption = new FileUtil.MatchOption(resourceDirectory);
        matchOption.fileSuffix = Constant.Symbol.DOT + Constant.File.XML;
        List<String> xmlFullFilenameList = FileUtil.findMatchFile(matchOption);
        if (xmlFullFilenameList != null) {
            String directory = new File(resourceDirectory).getAbsolutePath();
            for (String xmlFullFilename : xmlFullFilenameList) {
                File xmlFile = new File(xmlFullFilename);
                String parentFullFilename = xmlFile.getParent();
                File parentFile = new File(parentFullFilename);
                if (isAValuesDirectory(parentFile.getName())) {
                    // Ignore files under values* directories.
                    continue;
                }
                processXmlFile(xmlFullFilename, references, resourceCollector, directory);
            }
        }
    }

    private static void collectResources(String resourceDirectory, AaptResourceCollector resourceCollector) throws Exception {
        File resourceDirectoryFile = new File(resourceDirectory);
        String directory = resourceDirectoryFile.getAbsolutePath();
        File[] fileArray = resourceDirectoryFile.listFiles();
        if (fileArray != null) {
            for (File file : fileArray) {
                if (file.isDirectory()) {
                    String directoryName = file.getName();
                    if (directoryName.startsWith("values")) {
                        if (!isAValuesDirectory(directoryName)) {
                            throw new AaptUtilException("'" + directoryName + "' is not a valid values directory.");
                        }
                        processValues(file.getAbsolutePath(), resourceCollector, directory);
                    } else {
                        processFileNamesInDirectory(file.getAbsolutePath(), resourceCollector, directory);
                    }
                }
            }
        }
    }

    /**
     * is a value directory
     * 
     * @param directoryName
     * @return boolean
     */
    public static boolean isAValuesDirectory(String directoryName) {
        if (directoryName == null) {
            throw new NullPointerException("directoryName can not be null");
        }
        return directoryName.equals("values") || directoryName.startsWith("values-");
    }

    public static void processFileNamesInDirectory(String resourceDirectory, AaptResourceCollector resourceCollector, String directory) throws IOException {
        File resourceDirectoryFile = new File(resourceDirectory);
        String directoryName = resourceDirectoryFile.getName();
        int dashIndex = directoryName.indexOf('-');
        if (dashIndex != -1) {
            directoryName = directoryName.substring(0, dashIndex);
        }

        if (!RESOURCE_TYPES.containsKey(directoryName)) {
            throw new AaptUtilException(resourceDirectoryFile.getAbsolutePath() + " is not a valid resource sub-directory.");
        }
        File[] fileArray = resourceDirectoryFile.listFiles();
        if (fileArray != null) {
            for (File file : fileArray) {
                if (file.isHidden()) {
                    continue;
                }
                String filename = file.getName();
                int dotIndex = filename.indexOf('.');
                String resourceName = dotIndex != -1 ? filename.substring(0, dotIndex) : filename;

                RType rType = RESOURCE_TYPES.get(directoryName);
                resourceCollector.addIntResourceIfNotPresent(rType, resourceName, directory);
                ResourceDirectory resourceDirectoryBean = new ResourceDirectory(file.getParentFile().getName(), file.getAbsolutePath());
                resourceCollector.addRTypeResourceName(rType, resourceName, null, resourceDirectoryBean);
            }
        }
    }

    public static void processValues(String resourceDirectory, AaptResourceCollector resourceCollector, String directory) throws Exception {
        File resourceDirectoryFile = new File(resourceDirectory);
        File[] fileArray = resourceDirectoryFile.listFiles();
        if (fileArray != null) {
            for (File file : fileArray) {
                if (file.isHidden()) {
                    continue;
                }
                if (!file.isFile()) {
                    // warning
                    continue;
                }
                processValuesFile(file.getAbsolutePath(), resourceCollector, directory);
            }
        }
    }

    public static void processValuesFile(String valuesFullFilename, AaptResourceCollector resourceCollector, String directory) throws Exception {
        Document document = JavaXmlUtil.parse(valuesFullFilename);
        String directoryName = new File(valuesFullFilename).getParentFile().getName();
        Element root = document.getDocumentElement();

        for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            String resourceType = node.getNodeName();
            if (resourceType.equals(ITEM_TAG)) {
                resourceType = node.getAttributes().getNamedItem("type").getNodeValue();
                if (resourceType.equals("id")) {
                    resourceCollector.addIgnoreId(node.getAttributes().getNamedItem("name").getNodeValue());
                }
            }

            if (IGNORED_TAGS.contains(resourceType)) {
                continue;
            }

            if (!RESOURCE_TYPES.containsKey(resourceType)) {
                throw new AaptUtilException("Invalid resource type '<" + resourceType + ">' in '" + valuesFullFilename + "'.");
            }

            RType rType = RESOURCE_TYPES.get(resourceType);
            String resourceValue = null;
            switch (rType) {
            case STRING:
            case COLOR:
            case DIMEN:
            case DRAWABLE:
            case BOOL:
            case INTEGER:
                resourceValue = node.getTextContent().trim();
                break;
            case ARRAY:// has sub item
            case PLURALS:// has sub item
            case STYLE:// has sub item
            case STYLEABLE:// has sub item
                resourceValue = subNodeToString(node);
                break;
            case FRACTION:// no sub item
                resourceValue = nodeToString(node, true);
                break;
            case ATTR:// no sub item
                resourceValue = nodeToString(node, true);
                break;
            default:
                break;
            }
            try {
                addToResourceCollector(resourceCollector, new ResourceDirectory(directoryName, valuesFullFilename), node, rType, resourceValue, directory);
            } catch (Exception e) {
                throw new AaptUtilException(e.getMessage() + ",Process file error:" + valuesFullFilename, e);
            }
        }
    }

    public static void processXmlFile(String xmlFullFilename, List<RDotTxtEntry> references, AaptResourceCollector resourceCollector, String directory) throws IOException, XPathExpressionException {
        Document document = JavaXmlUtil.parse(xmlFullFilename);
        NodeList nodesWithIds = (NodeList) ANDROID_ID_DEFINITION.evaluate(document, XPathConstants.NODESET);
        for (int i = 0; i < nodesWithIds.getLength(); i++) {
            String resourceName = nodesWithIds.item(i).getNodeValue();
            if (!resourceName.startsWith(ID_DEFINITION_PREFIX)) {
                throw new AaptUtilException("Invalid definition of a resource: '" + resourceName + "'");
            }
            // Preconditions.checkState(resourceName.startsWith(ID_DEFINITION_PREFIX));

            resourceCollector.addIntResourceIfNotPresent(RType.ID, resourceName.substring(ID_DEFINITION_PREFIX.length()), directory);
        }

        NodeList nodesUsingIds = (NodeList) ANDROID_ID_USAGE.evaluate(document, XPathConstants.NODESET);
        for (int i = 0; i < nodesUsingIds.getLength(); i++) {
            String resourceName = nodesUsingIds.item(i).getNodeValue();
            // Preconditions.checkState(resourceName.charAt(0) == '@');
            int slashPosition = resourceName.indexOf('/');
            // Preconditions.checkState(slashPosition != -1);

            String rawRType = resourceName.substring(1, slashPosition);
            String name = resourceName.substring(slashPosition + 1);

            if (name.startsWith("android:")) {
                continue;
            }
            if (!RESOURCE_TYPES.containsKey(rawRType)) {
                throw new AaptUtilException("Invalid reference '" + resourceName + "' in '" + xmlFullFilename + "'");
            }
            RType rType = RESOURCE_TYPES.get(rawRType);

            // if(!resourceCollector.isContainResource(rType, IdType.INT,
            // sanitizeName(resourceCollector, name))){
            // throw new AaptUtilException("Not found reference '" +
            // resourceName + "' in '" + xmlFullFilename + "'");
            // }
            references.add(new FakeRDotTxtEntry(IdType.INT, rType, sanitizeName(resourceCollector, name)));
        }
    }

    private static void addToResourceCollector(AaptResourceCollector resourceCollector, ResourceDirectory resourceDirectory, Node node, RType rType, String resourceValue, String directory) {
        String resourceName = sanitizeName(resourceCollector, extractNameAttribute(node));
        resourceCollector.addRTypeResourceName(rType, resourceName, resourceValue, resourceDirectory);
        if (rType.equals(RType.STYLEABLE)) {

            int count = 0;
            for (Node attrNode = node.getFirstChild(); attrNode != null; attrNode = attrNode.getNextSibling()) {
                if (attrNode.getNodeType() != Node.ELEMENT_NODE || !attrNode.getNodeName().equals("attr")) {
                    continue;
                }

                String rawAttrName = extractNameAttribute(attrNode);
                String attrName = sanitizeName(resourceCollector, rawAttrName);
                resourceCollector.addResource(RType.STYLEABLE, IdType.INT, String.format("%s_%s", resourceName, attrName), Integer.toString(count++), directory);

                if (!rawAttrName.startsWith("android:")) {
                    resourceCollector.addIntResourceIfNotPresent(RType.ATTR, attrName, directory);
                    resourceCollector.addRTypeResourceName(RType.ATTR, rawAttrName, nodeToString(attrNode, true), resourceDirectory);
                }
            }

            resourceCollector.addIntArrayResourceIfNotPresent(rType, resourceName, count, directory);
        } else if (rType.equals(RType.STYLE)) {
            for (Node itemNode = node.getFirstChild(); itemNode != null; itemNode = itemNode.getNextSibling()) {
                if (itemNode.getNodeType() != Node.ELEMENT_NODE || !itemNode.getNodeName().equals(ITEM_TAG)) {
                    continue;
                }
                String textContent = itemNode.getTextContent();
                if (StringUtil.isNotBlank(textContent)) {
                    textContent = textContent.trim();
                }
                if (textContent.startsWith(Constant.Symbol.QUESTION_MARK) && textContent.indexOf(Constant.Symbol.SLASH_LEFT) > 0) {
                    int nameIndex = textContent.indexOf(Constant.Symbol.SLASH_LEFT);
                    String type = textContent.substring(1, nameIndex);
                    String name = textContent.substring(nameIndex + 1, textContent.length());
                    try {
                        RType itemRType = RType.valueOf(type.toUpperCase());
                        resourceCollector.addIntResourceIfNotPresent(itemRType, name, directory);
                    } catch (Exception e) {
                        continue;
                    }
                }
            }
            resourceCollector.addIntResourceIfNotPresent(rType, resourceName, directory);
        } else {
            resourceCollector.addIntResourceIfNotPresent(rType, resourceName, directory);
        }
    }

    private static String sanitizeName(AaptResourceCollector resourceCollector, String rawName) {
        String sanitizeName = rawName.replaceAll("[.:]", "_");
        resourceCollector.putSanitizeName(sanitizeName, rawName);
        return sanitizeName;
    }

    private static String extractNameAttribute(Node node) {
        return node.getAttributes().getNamedItem("name").getNodeValue();
    }

    /**
     * merge package r type resource map
     * 
     * @param packageRTypeResourceMapList
     * @return Map<String, Map<RType,Set<RDotTxtEntry>>>
     */
    public static Map<String, Map<RType, Set<RDotTxtEntry>>> mergePackageRTypeResourceMap(List<PackageRTypeResourceMap> packageRTypeResourceMapList) {
        Map<String, Map<RType, Set<RDotTxtEntry>>> packageRTypeResourceMergeMap = new HashMap<String, Map<RType, Set<RDotTxtEntry>>>();
        Map<String, AaptResourceCollector> aaptResourceCollectorMap = new HashMap<String, AaptResourceCollector>();
        for (PackageRTypeResourceMap packageRTypeResourceMap : packageRTypeResourceMapList) {
            String packageName = packageRTypeResourceMap.packageName;
            Map<RType, Set<RDotTxtEntry>> rTypeResourceMap = packageRTypeResourceMap.rTypeResourceMap;
            AaptResourceCollector aaptResourceCollector = null;
            if (aaptResourceCollectorMap.containsKey(packageName)) {
                aaptResourceCollector = aaptResourceCollectorMap.get(packageName);
            } else {
                aaptResourceCollector = new AaptResourceCollector();
                aaptResourceCollectorMap.put(packageName, aaptResourceCollector);
            }
            Iterator<Entry<RType, Set<RDotTxtEntry>>> iterator = rTypeResourceMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<RType, Set<RDotTxtEntry>> entry = iterator.next();
                RType rType = entry.getKey();
                Set<RDotTxtEntry> rDotTxtEntrySet = entry.getValue();
                for (RDotTxtEntry rDotTxtEntry : rDotTxtEntrySet) {
                    if (rDotTxtEntry.idType.equals(IdType.INT)) {
                        aaptResourceCollector.addIntResourceIfNotPresent(rType, rDotTxtEntry.name, null);
                    } else if (rDotTxtEntry.idType.equals(IdType.INT_ARRAY)) {
                        aaptResourceCollector.addResource(rType, rDotTxtEntry.idType, rDotTxtEntry.name, rDotTxtEntry.idValue, null);
                    }
                }
            }
        }
        Iterator<Entry<String, AaptResourceCollector>> iterator = aaptResourceCollectorMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<String, AaptResourceCollector> entry = iterator.next();
            packageRTypeResourceMergeMap.put(entry.getKey(), entry.getValue().getRTypeResourceMap());
        }
        return packageRTypeResourceMergeMap;
    }

    /**
     * write R.java
     * 
     * @param outputDirectory
     * @param packageName
     * @param rTypeResourceMap
     * @param isFinal
     */
    public static void writeRJava(String outputDirectory, String packageName, Map<RType, Set<RDotTxtEntry>> rTypeResourceMap, boolean isFinal) {
        String outputFullFilename = new File(outputDirectory).getAbsolutePath() + Constant.Symbol.SLASH_LEFT + (packageName.replace(Constant.Symbol.DOT, Constant.Symbol.SLASH_LEFT) + Constant.Symbol.SLASH_LEFT + "R" + Constant.Symbol.DOT + Constant.File.JAVA);
        FileUtil.createFile(outputFullFilename);
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileOutputStream(outputFullFilename));
            writer.format("package %s;\n\n", packageName);
            writer.println("public final class R {\n");
            for (RType rType : rTypeResourceMap.keySet()) {
                // Now start the block for the new type.
                writer.format("  public static final class %s {\n", rType.toString());
                for (RDotTxtEntry rDotTxtEntry : rTypeResourceMap.get(rType)) {
                    // Write out the resource.
                    // Write as an int.
                    writer.format("    public static%s%s %s=%s;\n", isFinal ? " final " : " ", rDotTxtEntry.idType, rDotTxtEntry.name, rDotTxtEntry.idValue);
                }
                writer.println("  }\n");
            }
            // Close the class definition.
            writer.println("}");
        } catch (Exception e) {
            throw new AaptUtilException(e);
        } finally {
            if (writer != null) {
                writer.flush();
                writer.close();
            }
        }
    }

    /**
     * write R.java
     * 
     * @param outputDirectory
     * @param packageRTypeResourceMap
     * @param isFinal
     * @throws IOException
     */
    public static void writeRJava(String outputDirectory, Map<String, Map<RType, Set<RDotTxtEntry>>> packageRTypeResourceMap, boolean isFinal) {
        for (String packageName : packageRTypeResourceMap.keySet()) {
            Map<RType, Set<RDotTxtEntry>> rTypeResourceMap = packageRTypeResourceMap.get(packageName);
            writeRJava(outputDirectory, packageName, rTypeResourceMap, isFinal);
        }
    }

    /**
     * sub node to string
     * 
     * @param node
     * @return String
     */
    private static String subNodeToString(Node node) {
        StringBuilder stringBuilder = new StringBuilder();
        if (node != null) {
            NodeList nodeList = node.getChildNodes();
            stringBuilder.append(nodeToString(node, false));
            stringBuilder.append(StringUtil.CRLF_STRING);
            int nodeListLength = nodeList.getLength();
            for (int i = 0; i < nodeListLength; i++) {
                Node childNode = nodeList.item(i);
                if (childNode.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                stringBuilder.append(nodeToString(childNode, true));
                stringBuilder.append(StringUtil.CRLF_STRING);
            }
            if (stringBuilder.length() > StringUtil.CRLF_STRING.length()) {
                stringBuilder.delete(stringBuilder.length() - StringUtil.CRLF_STRING.length(), stringBuilder.length());
            }
        }
        return stringBuilder.toString();
    }

    /**
     * node to String
     * 
     * @param node
     * @param isNoChild
     * @return String
     */
    private static String nodeToString(Node node, boolean isNoChild) {
        StringBuilder stringBuilder = new StringBuilder();
        if (node != null) {
            stringBuilder.append(node.getNodeName());
            NamedNodeMap namedNodeMap = node.getAttributes();
            stringBuilder.append(Constant.Symbol.MIDDLE_BRACKET_LEFT);
            int namedNodeMapLength = namedNodeMap.getLength();
            for (int j = 0; j < namedNodeMapLength; j++) {
                Node attributeNode = namedNodeMap.item(j);
                stringBuilder.append(Constant.Symbol.AT + attributeNode.getNodeName() + Constant.Symbol.EQUAL + attributeNode.getNodeValue());
                if (j < namedNodeMapLength - 1) {
                    stringBuilder.append(Constant.Symbol.COMMA);
                }
            }
            stringBuilder.append(Constant.Symbol.MIDDLE_BRACKET_RIGHT);
            String value = StringUtil.nullToBlank(isNoChild ? node.getTextContent() : node.getNodeValue()).trim();
            if (StringUtil.isNotBlank(value)) {
                stringBuilder.append(Constant.Symbol.EQUAL + value);
            }
        }
        return stringBuilder.toString();
    }

    /**
     * read r txt
     * 
     * @param rTxtFullFilename
     * @return Map<RType, Set<RDotTxtEntry>>
     */
    public static Map<RType, Set<RDotTxtEntry>> readRTxt(String rTxtFullFilename) {
        // read base resource entry
        Map<RType, Set<RDotTxtEntry>> rTypeResourceMap = new HashMap<RType, Set<RDotTxtEntry>>();
        if (StringUtil.isNotBlank(rTxtFullFilename) && FileUtil.isExist(rTxtFullFilename)) {
            BufferedReader bufferedReader = null;
            try {
                final Pattern TEXT_SYMBOLS_LINE = Pattern.compile("(\\S+) (\\S+) (\\S+) (.+)");
                bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(rTxtFullFilename)));
                String line = null;
                while ((line = bufferedReader.readLine()) != null) {
                    Matcher matcher = TEXT_SYMBOLS_LINE.matcher(line);
                    if (matcher.matches()) {
                        IdType idType = IdType.from(matcher.group(1));
                        RType rType = RType.valueOf(matcher.group(2).toUpperCase());
                        String name = matcher.group(3);
                        String idValue = matcher.group(4);
                        RDotTxtEntry rDotTxtEntry = new RDotTxtEntry(idType, rType, name, idValue);
                        Set<RDotTxtEntry> hashSet = null;
                        if (rTypeResourceMap.containsKey(rType)) {
                            hashSet = rTypeResourceMap.get(rType);
                        } else {
                            hashSet = new HashSet<RDotTxtEntry>();
                            rTypeResourceMap.put(rType, hashSet);
                        }
                        hashSet.add(rDotTxtEntry);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return rTypeResourceMap;
    }

    /**
     * generate public resource xml
     * 
     * @param aaptResourceCollector
     * @param outputIdsXmlFullFilename
     * @param outputPublicXmlFullFilename
     */
    public static void generatePublicResourceXml(AaptResourceCollector aaptResourceCollector, String outputIdsXmlFullFilename, String outputPublicXmlFullFilename) {
        if (aaptResourceCollector == null) {
            return;
        }
        FileUtil.createFile(outputIdsXmlFullFilename);
        FileUtil.createFile(outputPublicXmlFullFilename);
        PrintWriter idsWriter = null;
        PrintWriter publicWriter = null;
        try {
            FileUtil.createFile(outputIdsXmlFullFilename);
            FileUtil.createFile(outputPublicXmlFullFilename);
            idsWriter = new PrintWriter(new File(outputIdsXmlFullFilename), Constant.Encoding.UTF8);
            publicWriter = new PrintWriter(new File(outputPublicXmlFullFilename), Constant.Encoding.UTF8);
            idsWriter.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
            publicWriter.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
            idsWriter.println("<resources>");
            publicWriter.println("<resources>");
            Map<RType, Set<RDotTxtEntry>> map = aaptResourceCollector.getRTypeResourceMap();
            Iterator<Entry<RType, Set<RDotTxtEntry>>> iterator = map.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<RType, Set<RDotTxtEntry>> entry = iterator.next();
                RType rType = entry.getKey();
                if (!rType.equals(RType.STYLEABLE)) {
                    Set<RDotTxtEntry> set = entry.getValue();
                    for (RDotTxtEntry rDotTxtEntry : set) {
                        // if(rType.equals(RType.STYLE)){
                        String rawName = aaptResourceCollector.getRawName(rDotTxtEntry.name);
                        if (StringUtil.isBlank(rawName)) {
                            // System.err.println("Blank?"+rDotTxtEntry.name);
                            rawName = rDotTxtEntry.name;
                        }
                        publicWriter.println("<public type=\"" + rType + "\" name=\"" + rawName + "\" id=\"" + rDotTxtEntry.idValue + "\" />");
                        // }else{
                        // publicWriter.println("<public type=\""+rType+"\"
                        // name=\""+rDotTxtEntry.name+"\"
                        // id=\""+rDotTxtEntry.idValue+"\" />");
                        // }
                    }
                    for (RDotTxtEntry rDotTxtEntry : set) {
                        if (rType.equals(RType.ID)) {
                            idsWriter.println("<item type=\"" + rType + "\" name=\"" + rDotTxtEntry.name + "\"/>");
                        } else if (rType.equals(RType.STYLE)) {

                            if (rDotTxtEntry.name.indexOf(Constant.Symbol.UNDERLINE) > 0) {
                                // idsWriter.println("<item type=\""+rType+"\"
                                // name=\""+(rDotTxtEntry.name.replace(Constant.Symbol.UNDERLINE,
                                // Constant.Symbol.DOT))+"\"/>");
                            }
                        }
                    }
                }
                idsWriter.flush();
                publicWriter.flush();
            }
            idsWriter.println("</resources>");
            publicWriter.println("</resources>");
        } catch (Exception e) {
            throw new PatchUtilException(e);
        } finally {
            if (idsWriter != null) {
                idsWriter.flush();
                idsWriter.close();
            }
            if (publicWriter != null) {
                publicWriter.flush();
                publicWriter.close();
            }
        }
    }

    public static class PackageRTypeResourceMap {
        private String packageName = null;
        private Map<RType, Set<RDotTxtEntry>> rTypeResourceMap = null;

        public PackageRTypeResourceMap(String packageName, Map<RType, Set<RDotTxtEntry>> rTypeResourceMap) {
            this.packageName = packageName;
            this.rTypeResourceMap = rTypeResourceMap;
        }
    }

    public static class AaptUtilException extends RuntimeException {
        private static final long serialVersionUID = 1702278793911780809L;

        public AaptUtilException(String message) {
            super(message);
        }

        public AaptUtilException(Throwable cause) {
            super(cause);
        }

        public AaptUtilException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
