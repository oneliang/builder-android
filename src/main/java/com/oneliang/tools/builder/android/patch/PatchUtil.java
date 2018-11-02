package com.oneliang.tools.builder.android.patch;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.oneliang.Constants;
import com.oneliang.tools.builder.android.aapt.RDotTxtEntry;
import com.oneliang.tools.builder.android.aapt.RDotTxtEntry.RType;
import com.oneliang.tools.builder.android.aapt.ResourceEntry;
import com.oneliang.util.common.JavaXmlUtil;
import com.oneliang.util.common.ObjectUtil;
import com.oneliang.util.file.FileUtil;

public final class PatchUtil {

    public static void copyIncreasePublicResourceEntry(Map<RType, Set<RDotTxtEntry>> rTypeIncreaseResourceMap, String increaseResourceOutputDirectory, String inputIdsXmlFullFilename, String outputIdsXmlFullFilename, String inputPublicXmlFullFilename, String outputPublicXmlFullFilename) {
        Set<String> resourceNameSet = new HashSet<String>();
        Set<PublicResourceEntry> publicResourceEntrySet = new HashSet<PublicResourceEntry>();
        Iterator<Entry<RType, Set<RDotTxtEntry>>> rTypeIncreaseResourceIterator = rTypeIncreaseResourceMap.entrySet().iterator();
        while (rTypeIncreaseResourceIterator.hasNext()) {
            Entry<RType, Set<RDotTxtEntry>> rTypeIncreaseResourceEntry = rTypeIncreaseResourceIterator.next();
            Set<RDotTxtEntry> increaseResourceSet = rTypeIncreaseResourceEntry.getValue();
            for (RDotTxtEntry rDotTxtEntry : increaseResourceSet) {
                resourceNameSet.add(rDotTxtEntry.name);
                publicResourceEntrySet.add(new PublicResourceEntry(rTypeIncreaseResourceEntry.getKey(), rDotTxtEntry.name));
            }
        }
        if (!resourceNameSet.isEmpty()) {
            // increase id item
            copyIncreaseResourceEntryFromIdsXml(inputIdsXmlFullFilename, outputIdsXmlFullFilename, resourceNameSet);
            // increase public item
            copyIncreaseResourceEntryFromPublicXml(inputPublicXmlFullFilename, outputPublicXmlFullFilename, publicResourceEntrySet);
        }
    }

    public static void copyModifiedResourceEntry(RType rType, String resourceFullFilename, String outputDirectory, String resourceName, String newResourceName) {
        String directoryName = new File(resourceFullFilename).getParentFile().getName();
        switch (rType) {
        case STRING:
        case COLOR:
        case DIMEN:
        case DRAWABLE:
        case ARRAY:
        case PLURALS:
        case BOOL:
        case FRACTION:
        case STYLE:
        case STYLEABLE:
        case INTEGER:
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();
            Document document = JavaXmlUtil.parse(resourceFullFilename);
            String outputFullFilename = outputDirectory + Constants.Symbol.SLASH_LEFT + directoryName + Constants.Symbol.SLASH_LEFT + rType.toString() + "s.xml";
            Document outputDocument = null;
            if (FileUtil.isExist(outputFullFilename)) {
                outputDocument = JavaXmlUtil.parse(outputFullFilename);
            } else {
                outputDocument = JavaXmlUtil.getEmptyDocument();
                FileUtil.createFile(outputFullFilename);
            }
            try {
                Node rootElement = outputDocument.getDocumentElement();
                if (rootElement == null) {
                    rootElement = outputDocument.createElement("resources");// outputDocument.importNode(document.getDocumentElement(),false);
                    outputDocument.appendChild(rootElement);
                }
                if (rType.equals(RType.ARRAY) || rType.equals(RType.PLURALS) || rType.equals(RType.FRACTION) || rType.equals(RType.STYLE) || rType.equals(RType.STYLEABLE)) {
                    Node node = null;
                    if (rType.equals(RType.ARRAY)) {
                        node = (Node) xPath.evaluate("/resources/string-array[@name='" + resourceName + "']", document, XPathConstants.NODE);
                        if (node == null) {
                            node = (Node) xPath.evaluate("/resources/integer-array[@name='" + resourceName + "']", document, XPathConstants.NODE);
                        }
                    } else if (rType.equals(RType.PLURALS)) {
                        node = (Node) xPath.evaluate("/resources/plurals[@name='" + resourceName + "']", document, XPathConstants.NODE);
                    } else if (rType.equals(RType.FRACTION)) {
                        node = (Node) xPath.evaluate("/resources/item[@name='" + resourceName + "' and @type='fraction']", document, XPathConstants.NODE);
                    } else if (rType.equals(RType.STYLE)) {
                        node = (Node) xPath.evaluate("/resources/style[@name='" + resourceName + "']", document, XPathConstants.NODE);
                    } else if (rType.equals(RType.STYLEABLE)) {
                        node = (Node) xPath.evaluate("/resources/declare-styleable[@name='" + resourceName + "']", document, XPathConstants.NODE);
                    }
                    if (node != null) {
                        Element newElement = (Element) outputDocument.importNode(node, true);
                        NamedNodeMap namedNodeMap = newElement.getAttributes();
                        Node nameNode = namedNodeMap.getNamedItem("name");
                        if (nameNode != null) {
                            newElement.setAttribute(nameNode.getNodeName(), newResourceName);
                        }
                        if (rType.equals(RType.STYLEABLE)) {// declare-styleable
                                                            // need to modify
                                                            // the sub item name
                                                            // too
                            NodeList newNodeList = newElement.getElementsByTagName("attr");
                            for (int i = 0; i < newNodeList.getLength(); i++) {
                                Element newChildNode = (Element) newNodeList.item(i);
                                NamedNodeMap newNamedNodeMap = newChildNode.getAttributes();
                                Node newNameNode = newNamedNodeMap.getNamedItem("name");
                                if (newNameNode != null) {
                                    newChildNode.setAttribute(newNameNode.getNodeName(), "m_in_build_" + newNameNode.getNodeValue());
                                }
                            }
                        }
                        rootElement.appendChild(newElement);
                    }
                } else {
                    Node node = (Node) xPath.evaluate("/resources/" + rType.toString() + "[@name='" + resourceName + "']", document, XPathConstants.NODE);
                    if (node != null) {
                        Element newElement = outputDocument.createElement(rType.toString());
                        NamedNodeMap namedNodeMap = node.getAttributes();
                        Node nameNode = namedNodeMap.getNamedItem("name");
                        if (nameNode != null) {
                            newElement.setAttribute(nameNode.getNodeName(), newResourceName);
                        }
                        Node formattedNode = namedNodeMap.getNamedItem("formatted");
                        if (formattedNode != null) {
                            newElement.setAttribute(formattedNode.getNodeName(), formattedNode.getNodeValue());
                        }
                        newElement.setTextContent(node.getTextContent());
                        rootElement.appendChild(newElement);
                    }
                }
                JavaXmlUtil.saveDocument(outputDocument, outputFullFilename);
            } catch (Exception e) {
                throw new PatchUtilException(resourceFullFilename + " save to:" + outputFullFilename, e);
            }
            break;
        }
    }

    /**
     * copy increase resource entry
     * 
     * @param rType
     * @param resourceFullFilename
     * @param resourceNameSet
     * @param outputDirectory
     */
    public static void copyIncreaseResourceEntry(RType rType, String resourceFullFilename, Set<ResourceEntry> resourceEntrySet, String outputDirectory) {
        String directoryName = new File(resourceFullFilename).getParentFile().getName();
        switch (rType) {
        case STRING:
        case COLOR:
        case DIMEN:
        case DRAWABLE:
        case ARRAY:
        case PLURALS:
        case BOOL:
        case FRACTION:
        case STYLE:
        case STYLEABLE:
        case INTEGER:
        case ATTR:
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();
            Document document = JavaXmlUtil.parse(resourceFullFilename);
            String outputFullFilename = outputDirectory + Constants.Symbol.SLASH_LEFT + directoryName + Constants.Symbol.SLASH_LEFT + rType.toString() + "s.xml";
            Document outputDocument = null;
            if (FileUtil.isExist(outputFullFilename)) {
                outputDocument = JavaXmlUtil.parse(outputFullFilename);
            } else {
                outputDocument = JavaXmlUtil.getEmptyDocument();
                FileUtil.createFile(outputFullFilename);
            }
            try {
                if (resourceEntrySet != null && !resourceEntrySet.isEmpty()) {
                    Node rootElement = outputDocument.getDocumentElement();
                    if (rootElement == null) {
                        rootElement = outputDocument.createElement("resources");// outputDocument.importNode(document.getDocumentElement(),false);
                        outputDocument.appendChild(rootElement);
                    }
                    for (ResourceEntry resourceEntry : resourceEntrySet) {
                        if (rType.equals(RType.ARRAY) || rType.equals(RType.PLURALS) || rType.equals(RType.FRACTION) || rType.equals(RType.STYLE) || rType.equals(RType.STYLEABLE)) {
                            Node node = null;
                            if (rType.equals(RType.ARRAY)) {
                                node = (Node) xPath.evaluate("/resources/string-array[@name='" + resourceEntry.name + "']", document, XPathConstants.NODE);
                                if (node == null) {
                                    node = (Node) xPath.evaluate("/resources/integer-array[@name='" + resourceEntry.name + "']", document, XPathConstants.NODE);
                                }
                            } else if (rType.equals(RType.PLURALS)) {
                                node = (Node) xPath.evaluate("/resources/plurals[@name='" + resourceEntry.name + "']", document, XPathConstants.NODE);
                            } else if (rType.equals(RType.FRACTION)) {
                                node = (Node) xPath.evaluate("/resources/item[@name='" + resourceEntry.name + "' and @type='fraction']", document, XPathConstants.NODE);
                            } else if (rType.equals(RType.STYLE)) {
                                node = (Node) xPath.evaluate("/resources/style[@name='" + resourceEntry.name + "']", document, XPathConstants.NODE);
                            } else if (rType.equals(RType.STYLEABLE)) {
                                node = (Node) xPath.evaluate("/resources/declare-styleable[@name='" + resourceEntry.name + "']", document, XPathConstants.NODE);
                            }
                            if (node != null) {
                                Element newElement = (Element) outputDocument.importNode(node, true);
                                if (rType.equals(RType.STYLEABLE)) {// declare-styleable
                                                                    // need to
                                                                    // modify
                                                                    // the sub
                                                                    // item name
                                                                    // too
                                    NodeList newNodeList = newElement.getElementsByTagName("attr");
                                    for (int i = 0; i < newNodeList.getLength(); i++) {
                                        Element newChildNode = (Element) newNodeList.item(i);
                                        NamedNodeMap newNamedNodeMap = newChildNode.getAttributes();
                                        Node newNameNode = newNamedNodeMap.getNamedItem("name");
                                        if (newNameNode != null) {
                                            newChildNode.setAttribute(newNameNode.getNodeName(), "m_in_build_" + newNameNode.getNodeValue());
                                        }
                                    }
                                }
                                rootElement.appendChild(newElement);
                            }
                        } else {
                            Node node = (Node) xPath.evaluate("/resources/" + rType.toString() + "[@name='" + resourceEntry.name + "']", document, XPathConstants.NODE);
                            if (node != null) {
                                Element newElement = outputDocument.createElement(rType.toString());
                                NamedNodeMap namedNodeMap = node.getAttributes();
                                Node nameNode = namedNodeMap.getNamedItem("name");
                                if (nameNode != null) {
                                    newElement.setAttribute(nameNode.getNodeName(), nameNode.getNodeValue());
                                }
                                Node formattedNode = namedNodeMap.getNamedItem("formatted");
                                if (formattedNode != null) {
                                    newElement.setAttribute(formattedNode.getNodeName(), formattedNode.getNodeValue());
                                }
                                newElement.setTextContent(node.getTextContent());
                                rootElement.appendChild(newElement);
                            }
                        }
                    }
                    JavaXmlUtil.saveDocument(outputDocument, outputFullFilename);
                }
            } catch (Exception e) {
                throw new PatchUtilException(resourceFullFilename + " save to:" + outputFullFilename, e);
            }
            break;
        }
    }

    /**
     * copy increase resource from ids xml
     * 
     * @param inputXmlFullFilename
     * @param outputXmlFullFilename
     * @param resourceNameSet
     */
    public static void copyIncreaseResourceEntryFromIdsXml(String inputXmlFullFilename, String outputXmlFullFilename, Set<String> resourceNameSet) {
        if (FileUtil.isExist(inputXmlFullFilename)) {
            Document document = JavaXmlUtil.parse(inputXmlFullFilename);
            Document emptyDocument = JavaXmlUtil.getEmptyDocument();
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();
            try {
                if (resourceNameSet != null && !resourceNameSet.isEmpty()) {
                    String tagName = "item";
                    ;
                    Element rootElement = emptyDocument.createElement("resources");
                    emptyDocument.appendChild(rootElement);
                    NodeList nodeList = document.getElementsByTagName(tagName);
                    for (int i = 0; i < nodeList.getLength(); i++) {
                        Node node = nodeList.item(i);
                        if (node.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }
                        NamedNodeMap namedNodeMap = node.getAttributes();
                        if (namedNodeMap != null) {
                            String nodeAttributeName = namedNodeMap.getNamedItem("name").getNodeValue();
                            for (String resourceName : resourceNameSet) {
                                if (resourceName.equals(nodeAttributeName)) {
                                    rootElement.appendChild(emptyDocument.importNode(node, true));
                                    break;
                                }
                            }
                        }
                    }
                    // xpath is very slow in here
                    // for(String resourceName:resourceNameSet){
                    // Node node = (Node)
                    // xPath.evaluate("/resources/"+tagName+"[@name='"+resourceName+"']",
                    // document, XPathConstants.NODE);
                    // if(node!=null){
                    // rootElement.appendChild(emptyDocument.importNode(node,
                    // true));
                    // }
                    // }
                    FileUtil.createFile(outputXmlFullFilename);
                    JavaXmlUtil.saveDocument(emptyDocument, outputXmlFullFilename);
                }
            } catch (Exception e) {
                throw new PatchUtilException(e);
            }
        }
    }

    /**
     * copy increase resource from public xml
     * 
     * @param inputXmlFullFilename
     * @param outputXmlFullFilename
     * @param publicResourceNameSet
     */
    public static void copyIncreaseResourceEntryFromPublicXml(String inputXmlFullFilename, String outputXmlFullFilename, Set<PublicResourceEntry> publicResourceEntrySet) {
        if (FileUtil.isExist(inputXmlFullFilename)) {
            Document document = JavaXmlUtil.parse(inputXmlFullFilename);
            Document emptyDocument = JavaXmlUtil.getEmptyDocument();
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();
            try {
                if (publicResourceEntrySet != null && !publicResourceEntrySet.isEmpty()) {
                    String tagName = "public";
                    Element rootElement = emptyDocument.createElement("resources");
                    emptyDocument.appendChild(rootElement);
                    NodeList nodeList = document.getElementsByTagName(tagName);
                    for (int i = 0; i < nodeList.getLength(); i++) {
                        Node node = nodeList.item(i);
                        if (node.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }
                        NamedNodeMap namedNodeMap = node.getAttributes();
                        if (namedNodeMap != null) {
                            String nodeAttributeName = namedNodeMap.getNamedItem("name").getNodeValue();
                            String nodeAttributeType = namedNodeMap.getNamedItem("type").getNodeValue();
                            for (PublicResourceEntry publicResourceEntry : publicResourceEntrySet) {
                                if (publicResourceEntry.resourceName.equals(nodeAttributeName) && publicResourceEntry.rType.equals(RType.valueOf(nodeAttributeType.toUpperCase()))) {
                                    rootElement.appendChild(emptyDocument.importNode(node, true));
                                    break;
                                }
                            }
                        }
                    }
                    // xpath is very slow in here
                    // for(String resourceName:resourceNameSet){
                    // Node node = (Node)
                    // xPath.evaluate("/resources/"+tagName+"[@name='"+resourceName+"']",
                    // document, XPathConstants.NODE);
                    // if(node!=null){
                    // rootElement.appendChild(emptyDocument.importNode(node,
                    // true));
                    // }
                    // }
                    FileUtil.createFile(outputXmlFullFilename);
                    JavaXmlUtil.saveDocument(emptyDocument, outputXmlFullFilename);
                }
            } catch (Exception e) {
                throw new PatchUtilException(e);
            }
        }
    }

    public static class PublicResourceEntry {
        private RType rType = null;
        private String resourceName = null;

        public PublicResourceEntry(RType rType, String resourceName) {
            this.rType = rType;
            this.resourceName = resourceName;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof PublicResourceEntry)) {
                return false;
            }
            PublicResourceEntry that = (PublicResourceEntry) obj;
            return ObjectUtil.equal(this.rType, that.rType) && ObjectUtil.equal(this.resourceName, that.resourceName);
        }

        public int hashCode() {
            return Arrays.hashCode(new Object[] { this.rType, this.resourceName });
        }
    }

    public static class PatchUtilException extends RuntimeException {
        private static final long serialVersionUID = 5982003304074821184L;

        public PatchUtilException(String message) {
            super(message);
        }

        public PatchUtilException(Throwable cause) {
            super(cause);
        }

        public PatchUtilException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
