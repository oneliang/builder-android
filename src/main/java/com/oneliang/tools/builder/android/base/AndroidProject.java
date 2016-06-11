package com.oneliang.tools.builder.android.base;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.oneliang.Constant;
import com.oneliang.tools.builder.android.aapt.RDotTxtEntry;
import com.oneliang.tools.builder.android.aapt.RDotTxtEntry.RType;
import com.oneliang.tools.builder.base.BuildException;
import com.oneliang.tools.builder.java.base.JavaProject;
import com.oneliang.util.common.JavaXmlUtil;
import com.oneliang.util.file.FileUtil;

public class AndroidProject extends JavaProject{

	public static enum DirectoryType{
		SRC,RES,GEN,LIBS,ASSETS,CLASSES_OUTPUT,GEN_OUTPUT,ANDROID_MANIFEST
	}

	public static final int BUILD_TYPE_DEFAULT=0;
	public static final int BUILD_TYPE_ECLIPSE=1;

	static final String UNSIGNED_APK_SUFFIX="_unsigned.apk";
	static final String DEBUG_APK_SUFFIX="_debug.apk";
	static final String RELEASE_APK_SUFFIX="_release.apk";
	static final String DEBUG_ZIP_ALIGN_APK_SUFFIX="_debug_zipalign.apk";
	static final String RELEASE_ZIP_ALIGN_APK_SUFFIX="_release_zipalign.apk";

	public static final String ALL_CLASSES_JAR="allClasses.jar";
	public static final String ALL_ORIGINAL_CLASSES_JAR="allOriginalClasses.jar";
	public static final String AUTO_DEX_DEX_CLASSES_PREFIX="dexClasses";
	public static final String CLASSES_DEX="classes.dex";
	public static final String RESOURCE_FILE="resources.ap_";
	public static final String DIFFERENT_JAR="different.jar";
	public static final String IDS_XML="ids.xml";
	public static final String PUBLIC_XML="public.xml";

	static final String RES = "res";
	static final String GEN = "gen";
	private static final String LIBS = "libs";
	private static final String ASSETS = "assets";
	private static final String BIN="bin";
	
	static final String ANDROID_MANIFEST="AndroidManifest.xml";
	private static final String PROGUARD_CFG="proguard.cfg";

	private int buildType=BUILD_TYPE_DEFAULT;
	private int dexId = 0;
	private String compileTarget = null;
	private boolean debug=false;
	//project home directory
	private List<String> resourceDirectoryList=new ArrayList<String>();
	private String gen=null;
	private List<String> libsDirectoryList=new ArrayList<String>();
	private List<String> assetsDirectoryList=new ArrayList<String>();
	private String bin=null;
	//project home file and properties
	private List<String> androidManifestList=new ArrayList<String>();
	private String proguardCfg=null;
	private String packageName=null;
	//output directory
	private String genOutput=null;
	private String optimizedOutput=null;
	private String dexOutput=null;
	private String autoDexOutput=null;
	private String prepareAssetsOutput=null;
	private String prepareLibOutput=null;
	private String optimizedOriginalOutput=null;
	private String optimizedProguardOutput=null;
	private String patchOutput=null;
	private String patchPrepareOutput=null;
	private String patchDifferentOutput=null;
	private String resourceOutput=null;
	//output file
	private String unsignedApkFullFilename=null;
	private String apkFullFilename=null;
	private String zipAlignApkFullFilename=null;
	private String autoDexAllClassesJar=null;
	private String autoDexAllOriginalClassesJar=null;
	
	//use in building
	private boolean allCompileFileHasCache=false;
	private Map<RType,Set<RDotTxtEntry>> rTypeResourceMap=null;
	private List<String> thisTimeClassFileList=null;

	public AndroidProject() {
	}

	public AndroidProject(String workspace,String name) {
		this(workspace,name,workspace,BUILD_TYPE_ECLIPSE);
	}

	public AndroidProject(String workspace,String name,String outputHome,int buildType) {
		super(workspace,name,outputHome);
		if(buildType==BUILD_TYPE_DEFAULT){
			if(outputHome==null){
				throw new NullPointerException("outputHome is null");
			}
		}
		this.buildType=buildType<BUILD_TYPE_DEFAULT?BUILD_TYPE_DEFAULT:buildType;
	}

	public void initialize(){
		super.initialize();
		String src=this.home+"/"+SRC;
		if(FileUtil.isExist(src)){
			this.sourceDirectoryList.add(src);
		}
		String res=this.home+"/"+RES;
		if(FileUtil.isExist(res)){
			this.resourceDirectoryList.add(res);
		}
		this.gen=this.home+"/"+GEN;
		String libs=this.home+"/"+LIBS;
		if(FileUtil.isExist(libs)){
			this.libsDirectoryList.add(libs);
		}
		String assets=this.home+"/"+ASSETS;
		if(FileUtil.isExist(assets)){
			this.assetsDirectoryList.add(assets);
		}
		this.bin=this.home+"/"+BIN;
		if(this.buildType==BUILD_TYPE_ECLIPSE){
			//when build type is eclipse,change output home
			this.outputHome=this.outputHome+"/"+BIN;
			this.genOutput=this.gen;
			this.classesOutput=this.outputHome+"/"+CLASSES;
			this.cacheOutput=this.outputHome+"/"+CACHE;
		}else{
			this.genOutput=this.outputHome+"/"+GEN;
		}
		this.optimizedOutput=this.outputHome+"/optimized";
		this.dexOutput=this.outputHome+"/dex";
		this.autoDexOutput=this.outputHome+"/autoDex";
		this.prepareAssetsOutput=this.prepareOutput+"/assets";
		this.prepareLibOutput=this.prepareOutput+"/lib";

		this.optimizedOriginalOutput=this.optimizedOutput+"/original";
		this.optimizedProguardOutput=this.optimizedOutput+"/proguard";
		this.patchOutput=this.outputHome+"/patch";
		this.patchPrepareOutput=this.patchOutput+"/prepare";
		this.patchDifferentOutput=this.patchOutput+"/different";
		this.resourceOutput=this.outputHome+"/res";

		this.androidManifestList.add(this.home+"/"+ANDROID_MANIFEST);
		this.proguardCfg=this.home+"/"+PROGUARD_CFG;
		this.packageName=this.parsePackageName();
		
		this.unsignedApkFullFilename=this.outputHome+"/"+this.name+UNSIGNED_APK_SUFFIX;
		this.apkFullFilename=this.outputHome+"/"+this.name+(this.debug?DEBUG_APK_SUFFIX:RELEASE_APK_SUFFIX);
		this.zipAlignApkFullFilename=this.outputHome+"/"+this.name+(this.debug?DEBUG_ZIP_ALIGN_APK_SUFFIX:RELEASE_ZIP_ALIGN_APK_SUFFIX);
		this.autoDexAllClassesJar=this.autoDexOutput+"/"+ALL_CLASSES_JAR;
		this.autoDexAllOriginalClassesJar=this.autoDexOutput+"/"+ALL_ORIGINAL_CLASSES_JAR;
		
		for(String libsDirectory:this.libsDirectoryList){
			FileUtil.MatchOption matchOption=new FileUtil.MatchOption(libsDirectory);
			matchOption.fileSuffix=Constant.Symbol.DOT+Constant.File.JAR;
			this.dependJarList.addAll(FileUtil.findMatchFile(matchOption));
		}
	}

	/**
	 * parse package name
	 * @return String
	 */
	protected String parsePackageName(){
		String packageName=null;
		if(!this.androidManifestList.isEmpty()){
			String androidManifest=this.androidManifestList.get(0);
			if(FileUtil.isExist(androidManifest)){
				XPathFactory factory = XPathFactory.newInstance();
				XPath xpath = factory.newXPath();
				Document document = JavaXmlUtil.parse(androidManifest);
				try{
					XPathExpression expression = xpath.compile("/manifest[@package]");
					NodeList nodeList = (NodeList) expression.evaluate(document, XPathConstants.NODESET);
					if(nodeList!=null&&nodeList.getLength()>0){
						Node node=nodeList.item(0);
						packageName=node.getAttributes().getNamedItem("package").getTextContent();
					}
				}catch(Exception e){
					throw new BuildException(androidManifest,e);
				}
			}
		}
		return packageName;
	}

	public String toString() {
		StringBuilder stringBuilder=new StringBuilder();
		stringBuilder.append("name:"+this.name+",");
		stringBuilder.append("dexId:"+this.dexId+",");
		stringBuilder.append("buildType:"+this.buildType+",");
		stringBuilder.append("compileTarget:"+this.compileTarget+",");
		stringBuilder.append("classesOutput:"+this.classesOutput+",");
		stringBuilder.append("genOutput:"+this.genOutput+",");
		stringBuilder.append("cacheOutput:"+this.cacheOutput+",");
		if(this.sources!=null&&this.sources.length>0){
			stringBuilder.append("sources:[");
			for(String source:this.sources){
				stringBuilder.append(source+",");
			}
			stringBuilder.append("],");
		}
		if(this.dependProjects!=null&&this.dependProjects.length>0){
			stringBuilder.append("dependProjects:[");
			for(String dependProject:this.dependProjects){
				stringBuilder.append(dependProject+",");
			}
			stringBuilder.append("]");
		}
		return stringBuilder.toString();
	}

	/**
	 * @return the dexId
	 */
	public int getDexId() {
		return dexId;
	}

	/**
	 * @param dexId the dexId to set
	 */
	public void setDexId(int dexId) {
		this.dexId = dexId;
	}

	/**
	 * @return the compilePlatforms
	 */
	public String getCompileTarget() {
		return compileTarget;
	}

	/**
	 * @param compilePlatforms the compilePlatforms to set
	 */
	public void setCompileTarget(String compileTarget) {
		this.compileTarget = compileTarget;
	}

	/**
	 * @return the resourceDirectoryList
	 */
	public List<String> getResourceDirectoryList() {
		return this.resourceDirectoryList;
	}

	/**
	 * @return the gen
	 */
	public String getGen() {
		return gen;
	}

	/**
	 * @return the libsDirectoryList
	 */
	public List<String> getLibsDirectoryList() {
		return this.libsDirectoryList;
	}

	/**
	 * @return the assetsDirectoryList
	 */
	public List<String> getAssetsDirectoryList() {
		return this.assetsDirectoryList;
	}

	/**
	 * @return the androidManifestList
	 */
	public List<String> getAndroidManifestList() {
		return this.androidManifestList;
	}

	/**
	 * @return the buildType
	 */
	public int getBuildType() {
		return buildType;
	}

	/**
	 * @param buildType the buildType to set
	 */
	public void setBuildType(int buildType) {
		this.buildType = buildType;
	}

	/**
	 * @return the genOutput
	 */
	public String getGenOutput() {
		return genOutput;
	}

	/**
	 * @return the proguardCfg
	 */
	public String getProguardCfg() {
		return proguardCfg;
	}

	/**
	 * @return the optimizedOutput
	 */
	public String getOptimizedOutput() {
		return optimizedOutput;
	}

	/**
	 * @return the allCompileFileHasCache
	 */
	public boolean isAllCompileFileHasCache() {
		return allCompileFileHasCache;
	}

	/**
	 * @param allCompileFileHasCache the allCompileFileHasCache to set
	 */
	public void setAllCompileFileHasCache(boolean allCompileFileHasCache) {
		this.allCompileFileHasCache = allCompileFileHasCache;
	}

	/**
	 * @return the packageName
	 */
	public String getPackageName() {
		return packageName;
	}

	/**
	 * @param packageName the packageName to set
	 */
	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	/**
	 * @return the dexOutput
	 */
	public String getDexOutput() {
		return dexOutput;
	}

	/**
	 * @return the autoDexOutput
	 */
	public String getAutoDexOutput() {
		return autoDexOutput;
	}

	/**
	 * @return the optimizedOriginalOutput
	 */
	public String getOptimizedOriginalOutput() {
		return optimizedOriginalOutput;
	}

	/**
	 * @return the optimizedProguardOutput
	 */
	public String getOptimizedProguardOutput() {
		return optimizedProguardOutput;
	}

	/**
	 * @return the unsignedApkFullFilename
	 */
	public String getUnsignedApkFullFilename() {
		return unsignedApkFullFilename;
	}

	/**
	 * @return the zipAlignApkFullFilename
	 */
	public String getZipAlignApkFullFilename() {
		return zipAlignApkFullFilename;
	}

	/**
	 * @return the apkFullFilename
	 */
	public String getApkFullFilename() {
		return apkFullFilename;
	}

	/**
	 * @param debug the debug to set
	 */
	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	/**
	 * @return the autoDexAllClassesJar
	 */
	public String getAutoDexAllClassesJar() {
		return autoDexAllClassesJar;
	}

	/**
	 * @return the bin
	 */
	public String getBin() {
		return bin;
	}

	/**
	 * @return the prepareAssetsOutput
	 */
	public String getPrepareAssetsOutput() {
		return prepareAssetsOutput;
	}

	/**
	 * @return the prepareLibOutput
	 */
	public String getPrepareLibOutput() {
		return prepareLibOutput;
	}

	/**
	 * @return the rTypeResourceMap
	 */
	public Map<RType, Set<RDotTxtEntry>> getRTypeResourceMap() {
		return rTypeResourceMap;
	}

	/**
	 * @param rTypeResourceMap the rTypeResourceMap to set
	 */
	public void setRTypeResourceMap(Map<RType, Set<RDotTxtEntry>> rTypeResourceMap) {
		this.rTypeResourceMap = rTypeResourceMap;
	}

	/**
	 * @return the autoDexAllOriginalClassesJar
	 */
	public String getAutoDexAllOriginalClassesJar() {
		return autoDexAllOriginalClassesJar;
	}

	/**
	 * @return the patchOutput
	 */
	public String getPatchOutput() {
		return patchOutput;
	}

	/**
	 * @return the patchPrepareOutput
	 */
	public String getPatchPrepareOutput() {
		return patchPrepareOutput;
	}

	/**
	 * @return the patchDifferentOutput
	 */
	public String getPatchDifferentOutput() {
		return patchDifferentOutput;
	}

	/**
	 * @return the thisTimeClassFileList
	 */
	public List<String> getThisTimeClassFileList() {
		return thisTimeClassFileList;
	}

	/**
	 * @param thisTimeClassFileList the thisTimeClassFileList to set
	 */
	public void setThisTimeClassFileList(List<String> thisTimeClassFileList) {
		this.thisTimeClassFileList = thisTimeClassFileList;
	}

	/**
	 * @return the resourceOutput
	 */
	public String getResourceOutput() {
		return resourceOutput;
	}
}
