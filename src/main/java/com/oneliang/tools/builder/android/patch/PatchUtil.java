package com.oneliang.tools.builder.android.patch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.oneliang.Constant;
import com.oneliang.tools.builder.android.aapt.AaptResourceCollector;
import com.oneliang.tools.builder.android.aapt.RDotTxtEntry;
import com.oneliang.tools.builder.android.aapt.RDotTxtEntry.IdType;
import com.oneliang.tools.builder.android.aapt.RDotTxtEntry.RType;
import com.oneliang.tools.builder.android.aapt.ResourceEntry;
import com.oneliang.util.common.JavaXmlUtil;
import com.oneliang.util.common.ObjectUtil;
import com.oneliang.util.common.StringUtil;
import com.oneliang.util.file.FileUtil;

public final class PatchUtil {

	/**
	 * read r txt
	 * @param rTxtFullFilename
	 * @return Map<RType, Set<RDotTxtEntry>>
	 */
	public static Map<RType, Set<RDotTxtEntry>> readRTxt(String rTxtFullFilename){
		//read base resource entry
		Map<RType, Set<RDotTxtEntry>> rTypeResourceMap=new HashMap<RType, Set<RDotTxtEntry>>();
		if(StringUtil.isNotBlank(rTxtFullFilename)&&FileUtil.isExist(rTxtFullFilename)){
			BufferedReader bufferedReader=null;
			try {
				final Pattern TEXT_SYMBOLS_LINE = Pattern.compile("(\\S+) (\\S+) (\\S+) (.+)");
				bufferedReader=new BufferedReader(new InputStreamReader(new FileInputStream(rTxtFullFilename)));
				String line=null;
				while((line=bufferedReader.readLine())!=null){
					Matcher matcher = TEXT_SYMBOLS_LINE.matcher(line);
					if (matcher.matches()) {
						IdType idType = IdType.from(matcher.group(1));
						RType rType = RType.valueOf(matcher.group(2).toUpperCase());
						String name = matcher.group(3);
						String idValue = matcher.group(4);
						RDotTxtEntry rDotTxtEntry=new RDotTxtEntry(idType, rType, name, idValue);
						Set<RDotTxtEntry> hashSet=null;
						if(rTypeResourceMap.containsKey(rType)){
							hashSet=rTypeResourceMap.get(rType);
						}else{
							hashSet=new HashSet<RDotTxtEntry>();
							rTypeResourceMap.put(rType, hashSet);
						}
						hashSet.add(rDotTxtEntry);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally{
				if(bufferedReader!=null){
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

	public static void copyIncreasePublicResourceEntry(Map<RType, Set<RDotTxtEntry>> rTypeIncreaseResourceMap, String increaseResourceOutputDirectory, String inputIdsXmlFullFilename, String outputIdsXmlFullFilename, String inputPublicXmlFullFilename, String outputPublicXmlFullFilename){
		Set<String> resourceNameSet=new HashSet<String>();
		Set<PublicResourceEntry> publicResourceEntrySet=new HashSet<PublicResourceEntry>();
		Iterator<Entry<RType, Set<RDotTxtEntry>>> rTypeIncreaseResourceIterator=rTypeIncreaseResourceMap.entrySet().iterator();
		while(rTypeIncreaseResourceIterator.hasNext()){
			Entry<RType, Set<RDotTxtEntry>> rTypeIncreaseResourceEntry=rTypeIncreaseResourceIterator.next();
			Set<RDotTxtEntry> increaseResourceSet=rTypeIncreaseResourceEntry.getValue();
			for(RDotTxtEntry rDotTxtEntry:increaseResourceSet){
				resourceNameSet.add(rDotTxtEntry.name);
				publicResourceEntrySet.add(new PublicResourceEntry(rTypeIncreaseResourceEntry.getKey(),rDotTxtEntry.name));
			}
		}
		if(!resourceNameSet.isEmpty()){
			//increase id item
			copyIncreaseResourceEntryFromIdsXml(inputIdsXmlFullFilename, outputIdsXmlFullFilename, resourceNameSet);
			//increase public item
			copyIncreaseResourceEntryFromPublicXml(inputPublicXmlFullFilename, outputPublicXmlFullFilename, publicResourceEntrySet);
		}
	}

	public static void copyModifiedResourceEntry(RType rType,String resourceFullFilename,String outputDirectory,String resourceName,String newResourceName){
		String directoryName=new File(resourceFullFilename).getParentFile().getName();
		switch(rType){
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
			Document document=JavaXmlUtil.parse(resourceFullFilename);
			String outputFullFilename=outputDirectory+Constant.Symbol.SLASH_LEFT+directoryName+Constant.Symbol.SLASH_LEFT+rType.toString()+"s.xml";
			Document outputDocument=null;
			if(FileUtil.isExist(outputFullFilename)){
				outputDocument=JavaXmlUtil.parse(outputFullFilename);
			}else{
				outputDocument=JavaXmlUtil.getEmptyDocument();
				FileUtil.createFile(outputFullFilename);
			}
			try {
				Node rootElement=outputDocument.getDocumentElement();
				if(rootElement==null){
					rootElement=outputDocument.createElement("resources");//outputDocument.importNode(document.getDocumentElement(),false);
					outputDocument.appendChild(rootElement);
				}
				if(rType.equals(RType.ARRAY)||rType.equals(RType.PLURALS)||rType.equals(RType.FRACTION)||rType.equals(RType.STYLE)||rType.equals(RType.STYLEABLE)){
					Node node=null;
					if(rType.equals(RType.ARRAY)){
						node = (Node) xPath.evaluate("/resources/string-array[@name='"+resourceName+"']", document, XPathConstants.NODE);
						if(node==null){
							node = (Node) xPath.evaluate("/resources/integer-array[@name='"+resourceName+"']", document, XPathConstants.NODE);
						}
					}else if(rType.equals(RType.PLURALS)){
						node = (Node) xPath.evaluate("/resources/plurals[@name='"+resourceName+"']", document, XPathConstants.NODE);
					}else if(rType.equals(RType.FRACTION)){
						node = (Node) xPath.evaluate("/resources/item[@name='"+resourceName+"' and @type='fraction']", document, XPathConstants.NODE);
					}else if(rType.equals(RType.STYLE)){
						node = (Node) xPath.evaluate("/resources/style[@name='"+resourceName+"']", document, XPathConstants.NODE);
					}else if(rType.equals(RType.STYLEABLE)){
						node = (Node) xPath.evaluate("/resources/declare-styleable[@name='"+resourceName+"']", document, XPathConstants.NODE);
					}
					if(node!=null){
						Element newElement=(Element)outputDocument.importNode(node, true);
						NamedNodeMap namedNodeMap=newElement.getAttributes();
						Node nameNode=namedNodeMap.getNamedItem("name");
						if(nameNode!=null){
							newElement.setAttribute(nameNode.getNodeName(), newResourceName);
						}
						if(rType.equals(RType.STYLEABLE)){//declare-styleable need to modify the sub item name too
							NodeList newNodeList=newElement.getElementsByTagName("attr");
							for(int i=0;i<newNodeList.getLength();i++){
								Element newChildNode=(Element)newNodeList.item(i);
								NamedNodeMap newNamedNodeMap=newChildNode.getAttributes();
								Node newNameNode=newNamedNodeMap.getNamedItem("name");
								if(newNameNode!=null){
									newChildNode.setAttribute(newNameNode.getNodeName(), "m_in_build_"+newNameNode.getNodeValue());
								}
							}
						}
						rootElement.appendChild(newElement);
					}
				}else{
					Node node = (Node) xPath.evaluate("/resources/"+rType.toString()+"[@name='"+resourceName+"']", document, XPathConstants.NODE);
					if(node!=null){
						Element newElement=outputDocument.createElement(rType.toString());
						NamedNodeMap namedNodeMap=node.getAttributes();
						Node nameNode=namedNodeMap.getNamedItem("name");
						if(nameNode!=null){
							newElement.setAttribute(nameNode.getNodeName(), newResourceName);
						}
						Node formattedNode=namedNodeMap.getNamedItem("formatted");
						if(formattedNode!=null){
							newElement.setAttribute(formattedNode.getNodeName(), formattedNode.getNodeValue());
						}
						newElement.setTextContent(node.getTextContent());
						rootElement.appendChild(newElement);
					}
				}
				JavaXmlUtil.saveDocument(outputDocument, outputFullFilename);
			} catch (Exception e) {
				throw new PatchUtilException(resourceFullFilename+" save to:"+outputFullFilename,e);
			}
			break;
		}
	}

	/**
	 * copy increase resource entry
	 * @param rType
	 * @param resourceFullFilename
	 * @param resourceNameSet
	 * @param outputDirectory
	 */
	public static void copyIncreaseResourceEntry(RType rType,String resourceFullFilename,Set<ResourceEntry> resourceEntrySet,String outputDirectory){
		String directoryName=new File(resourceFullFilename).getParentFile().getName();
		switch(rType){
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
			Document document=JavaXmlUtil.parse(resourceFullFilename);
			String outputFullFilename=outputDirectory+Constant.Symbol.SLASH_LEFT+directoryName+Constant.Symbol.SLASH_LEFT+rType.toString()+"s.xml";
			Document outputDocument=null;
			if(FileUtil.isExist(outputFullFilename)){
				outputDocument=JavaXmlUtil.parse(outputFullFilename);
			}else{
				outputDocument=JavaXmlUtil.getEmptyDocument();
				FileUtil.createFile(outputFullFilename);
			}
			try {
				if(resourceEntrySet!=null&&!resourceEntrySet.isEmpty()){
					Node rootElement=outputDocument.getDocumentElement();
					if(rootElement==null){
						rootElement=outputDocument.createElement("resources");//outputDocument.importNode(document.getDocumentElement(),false);
						outputDocument.appendChild(rootElement);
					}
					for(ResourceEntry resourceEntry:resourceEntrySet){
						if(rType.equals(RType.ARRAY)||rType.equals(RType.PLURALS)||rType.equals(RType.FRACTION)||rType.equals(RType.STYLE)||rType.equals(RType.STYLEABLE)){
							Node node=null;
							if(rType.equals(RType.ARRAY)){
								node = (Node) xPath.evaluate("/resources/string-array[@name='"+resourceEntry.name+"']", document, XPathConstants.NODE);
								if(node==null){
									node = (Node) xPath.evaluate("/resources/integer-array[@name='"+resourceEntry.name+"']", document, XPathConstants.NODE);
								}
							}else if(rType.equals(RType.PLURALS)){
								node = (Node) xPath.evaluate("/resources/plurals[@name='"+resourceEntry.name+"']", document, XPathConstants.NODE);
							}else if(rType.equals(RType.FRACTION)){
								node = (Node) xPath.evaluate("/resources/item[@name='"+resourceEntry.name+"' and @type='fraction']", document, XPathConstants.NODE);
							}else if(rType.equals(RType.STYLE)){
								node = (Node) xPath.evaluate("/resources/style[@name='"+resourceEntry.name+"']", document, XPathConstants.NODE);
							}else if(rType.equals(RType.STYLEABLE)){
								node = (Node) xPath.evaluate("/resources/declare-styleable[@name='"+resourceEntry.name+"']", document, XPathConstants.NODE);
							}
							if(node!=null){
								Element newElement=(Element)outputDocument.importNode(node, true);
								if(rType.equals(RType.STYLEABLE)){//declare-styleable need to modify the sub item name too
									NodeList newNodeList=newElement.getElementsByTagName("attr");
									for(int i=0;i<newNodeList.getLength();i++){
										Element newChildNode=(Element)newNodeList.item(i);
										NamedNodeMap newNamedNodeMap=newChildNode.getAttributes();
										Node newNameNode=newNamedNodeMap.getNamedItem("name");
										if(newNameNode!=null){
											newChildNode.setAttribute(newNameNode.getNodeName(), "m_in_build_"+newNameNode.getNodeValue());
										}
									}
								}
								rootElement.appendChild(newElement);
							}
						}else{
							Node node = (Node) xPath.evaluate("/resources/"+rType.toString()+"[@name='"+resourceEntry.name+"']", document, XPathConstants.NODE);
							if(node!=null){
								Element newElement=outputDocument.createElement(rType.toString());
								NamedNodeMap namedNodeMap=node.getAttributes();
								Node nameNode=namedNodeMap.getNamedItem("name");
								if(nameNode!=null){
									newElement.setAttribute(nameNode.getNodeName(), nameNode.getNodeValue());
								}
								Node formattedNode=namedNodeMap.getNamedItem("formatted");
								if(formattedNode!=null){
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
				throw new PatchUtilException(resourceFullFilename+" save to:"+outputFullFilename,e);
			}
			break;
		}
	}

	/**
	 * copy increase resource from ids xml
	 * @param inputXmlFullFilename
	 * @param outputXmlFullFilename
	 * @param resourceNameSet
	 */
	public static void copyIncreaseResourceEntryFromIdsXml(String inputXmlFullFilename,String outputXmlFullFilename, Set<String> resourceNameSet){
		if(FileUtil.isExist(inputXmlFullFilename)){
			Document document=JavaXmlUtil.parse(inputXmlFullFilename);
			Document emptyDocument=JavaXmlUtil.getEmptyDocument();
			XPathFactory xPathFactory = XPathFactory.newInstance();
			XPath xPath = xPathFactory.newXPath();
			try {
				if(resourceNameSet!=null&&!resourceNameSet.isEmpty()){
					String tagName="item";;
					Element rootElement=emptyDocument.createElement("resources");
					emptyDocument.appendChild(rootElement);
					NodeList nodeList=document.getElementsByTagName(tagName);
					for(int i=0;i<nodeList.getLength();i++){
						Node node=nodeList.item(i);
						if (node.getNodeType() != Node.ELEMENT_NODE) {
							continue;
						}
						NamedNodeMap namedNodeMap=node.getAttributes();
						if(namedNodeMap!=null){
							String nodeAttributeName=namedNodeMap.getNamedItem("name").getNodeValue();
							for(String resourceName:resourceNameSet){
								if(resourceName.equals(nodeAttributeName)){
									rootElement.appendChild(emptyDocument.importNode(node, true));
									break;
								}
							}
						}
					}
					//xpath is very slow in here
//					for(String resourceName:resourceNameSet){
//						Node node = (Node) xPath.evaluate("/resources/"+tagName+"[@name='"+resourceName+"']", document, XPathConstants.NODE);
//						if(node!=null){
//							rootElement.appendChild(emptyDocument.importNode(node, true));
//						}
//					}
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
	 * @param inputXmlFullFilename
	 * @param outputXmlFullFilename
	 * @param publicResourceNameSet
	 */
	public static void copyIncreaseResourceEntryFromPublicXml(String inputXmlFullFilename,String outputXmlFullFilename, Set<PublicResourceEntry> publicResourceEntrySet){
		if(FileUtil.isExist(inputXmlFullFilename)){
			Document document=JavaXmlUtil.parse(inputXmlFullFilename);
			Document emptyDocument=JavaXmlUtil.getEmptyDocument();
			XPathFactory xPathFactory = XPathFactory.newInstance();
			XPath xPath = xPathFactory.newXPath();
			try {
				if(publicResourceEntrySet!=null&&!publicResourceEntrySet.isEmpty()){
					String tagName="public";
					Element rootElement=emptyDocument.createElement("resources");
					emptyDocument.appendChild(rootElement);
					NodeList nodeList=document.getElementsByTagName(tagName);
					for(int i=0;i<nodeList.getLength();i++){
						Node node=nodeList.item(i);
						if (node.getNodeType() != Node.ELEMENT_NODE) {
							continue;
						}
						NamedNodeMap namedNodeMap=node.getAttributes();
						if(namedNodeMap!=null){
							String nodeAttributeName=namedNodeMap.getNamedItem("name").getNodeValue();
							String nodeAttributeType=namedNodeMap.getNamedItem("type").getNodeValue();
							for(PublicResourceEntry publicResourceEntry:publicResourceEntrySet){
								if(publicResourceEntry.resourceName.equals(nodeAttributeName)&&publicResourceEntry.rType.equals(RType.valueOf(nodeAttributeType.toUpperCase()))){
									rootElement.appendChild(emptyDocument.importNode(node, true));
									break;
								}
							}
						}
					}
					//xpath is very slow in here
//					for(String resourceName:resourceNameSet){
//						Node node = (Node) xPath.evaluate("/resources/"+tagName+"[@name='"+resourceName+"']", document, XPathConstants.NODE);
//						if(node!=null){
//							rootElement.appendChild(emptyDocument.importNode(node, true));
//						}
//					}
					FileUtil.createFile(outputXmlFullFilename);
					JavaXmlUtil.saveDocument(emptyDocument, outputXmlFullFilename);
				}
			} catch (Exception e) {
				throw new PatchUtilException(e);
			}
		}
	}

	/**
	 * generate public resource xml
	 * @param aaptResourceCollector
	 * @param outputIdsXmlFullFilename
	 * @param outputPublicXmlFullFilename
	 */
	public static void generatePublicResourceXml(AaptResourceCollector aaptResourceCollector,String outputIdsXmlFullFilename,String outputPublicXmlFullFilename){
		if(aaptResourceCollector==null){
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
			Map<RType, Set<RDotTxtEntry>> map=aaptResourceCollector.getRTypeResourceMap();
			Iterator<Entry<RType, Set<RDotTxtEntry>>> iterator=map.entrySet().iterator();
			while(iterator.hasNext()){
				Entry<RType, Set<RDotTxtEntry>> entry=iterator.next();
				RType rType=entry.getKey();
				if(!rType.equals(RType.STYLEABLE)){
					Set<RDotTxtEntry> set=entry.getValue();
					for(RDotTxtEntry rDotTxtEntry:set){
//						if(rType.equals(RType.STYLE)){
							String rawName=aaptResourceCollector.getRawName(rDotTxtEntry.name);
							if(StringUtil.isBlank(rawName)){
								//System.err.println("Blank?"+rDotTxtEntry.name);
								rawName=rDotTxtEntry.name;
							}
							publicWriter.println("<public type=\""+rType+"\" name=\""+rawName+"\" id=\""+rDotTxtEntry.idValue+"\" />");
//						}else{
//							publicWriter.println("<public type=\""+rType+"\" name=\""+rDotTxtEntry.name+"\" id=\""+rDotTxtEntry.idValue+"\" />");
//						}
					}
					for(RDotTxtEntry rDotTxtEntry:set){
						if(rType.equals(RType.ID)){
							idsWriter.println("<item type=\""+rType+"\" name=\""+rDotTxtEntry.name+"\"/>");
						}else if(rType.equals(RType.STYLE)){
							
							if(rDotTxtEntry.name.indexOf(Constant.Symbol.UNDERLINE)>0){
//								idsWriter.println("<item type=\""+rType+"\" name=\""+(rDotTxtEntry.name.replace(Constant.Symbol.UNDERLINE, Constant.Symbol.DOT))+"\"/>");
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
		} finally{
			if(idsWriter!=null){
				idsWriter.flush();
				idsWriter.close();
			}
			if(publicWriter!=null){
				publicWriter.flush();
				publicWriter.close();
			}
		}
	}

	public static class PublicResourceEntry{
		private RType rType=null;
		private String resourceName=null;
		public PublicResourceEntry(RType rType,String resourceName) {
			this.rType=rType;
			this.resourceName=resourceName;
		}
		public boolean equals(Object obj) {
			if(!(obj instanceof PublicResourceEntry)){
				return false;
			}
			PublicResourceEntry that=(PublicResourceEntry)obj;
			return ObjectUtil.equal(this.rType, that.rType)&&ObjectUtil.equal(this.resourceName, that.resourceName);
		}
		public int hashCode() {
			return Arrays.hashCode(new Object[]{this.rType,this.resourceName});
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
