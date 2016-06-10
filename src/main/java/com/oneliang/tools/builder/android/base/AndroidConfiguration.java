package com.oneliang.tools.builder.android.base;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.oneliang.Constant;
import com.oneliang.tools.builder.android.handler.AndroidProjectHandler;
import com.oneliang.tools.builder.android.handler.MultiAndroidProjectDexHandler;
import com.oneliang.tools.builder.base.BuildException;
import com.oneliang.tools.builder.base.BuilderConfiguration.HandlerBean;
import com.oneliang.tools.builder.base.BuilderConfiguration.TaskNodeInsertBean;
import com.oneliang.tools.builder.base.Handler;
import com.oneliang.tools.builder.base.Project;
import com.oneliang.tools.builder.java.base.JavaConfiguration;
import com.oneliang.util.common.JavaXmlUtil;
import com.oneliang.util.common.StringUtil;
import com.oneliang.util.file.FileUtil;
import com.oneliang.util.logging.Logger;
import com.oneliang.util.logging.LoggerManager;

public abstract class AndroidConfiguration extends JavaConfiguration{

	protected static final Logger logger=LoggerManager.getLogger(AndroidConfiguration.class);

	public static final String MAP_KEY_NEED_TO_CLEAN="needToClean";
	public static final String MAP_KEY_ANDROID_SDK="androidSdk";
	public static final String MAP_KEY_ANDROID_BUILD_TOOLS_VERSION="androidBuildToolsVersion";
	public static final String MAP_KEY_PROJECT_MAIN_PROPERTIES="projectMainProperties";
	public static final String MAP_KEY_PROJECT_WORKSPACE="projectWorkspace";
	public static final String MAP_KEY_PROJECT_IDE="projectIde";
	public static final String MAP_KEY_PROJECT_LOAD_MODE_ATTACH_BASE_CONTEXT_MULTI_DEX="projectLoadModeAttachBaseContextMultiDex";
	public static final String MAP_KEY_BUILD_OUTPUT="buildOutput";
	public static final String MAP_KEY_BUILD_OUTPUT_ECLIPSE="buildOutputEclipse";
	public static final String MAP_KEY_APK_DEBUG="apkDebug";
	public static final String MAP_KEY_APK_PRE_RELEASE="apkPreRelease";
	public static final String MAP_KEY_CUSTOM_PROJECT="customProject.";
	public static final String MAP_KEY_MULTI_DEX="multiDex.";
	public static final String MAP_KEY_AUTO_DEX="autoDex";
	public static final String MAP_KEY_AUTO_DEX_LINEAR_ALLOC_LIMIT="autoDexLinearAllocLimit";
	public static final String MAP_KEY_AUTO_DEX_FIELD_LIMIT="autoDexFieldLimit";
	public static final String MAP_KEY_AUTO_DEX_METHOD_LIMIT="autoDexMethodLimit";
	public static final String MAP_KEY_AUTO_DEX_MAIN_DEX_OTHER_CLASSES="autoDexMainDexOtherClasses";
	public static final String MAP_KEY_AAPT_GENERATE_R_FILE="aaptGenerateRFile";
	public static final String MAP_KEY_APK_PATCH_INPUT_ALL_CLASSES_JAR="apkPatchInputAllClassesJar";
	public static final String MAP_KEY_APK_PATCH_INPUT_R_TXT="apkPatchInputRTxt";
	public static final String MAP_KEY_APK_PATCH_BASE_APK="apkPatchBaseApk";
	public static final String MAP_KEY_APK_PATCH_RESOURCE_ITEM_MAPPING="apkPatchResourceItemMapping";
	public static final String MAP_KEY_PROGUARD_CLASSPATH = "proguardClasspath";

	protected boolean needToClean=true;
	protected String androidSdk=null;
	protected String androidBuildToolsVersion=null;
	protected String projectMainProperties=null;
	protected boolean projectLoadModeAttachBaseContextMultiDex=false;
	protected boolean buildOutputEclipse=false;
	protected boolean apkDebug=false;
	protected boolean apkPreRelease=false;
	protected boolean autoDex=false;
	protected int autoDexLinearAllocLimit=Integer.MAX_VALUE;
	protected int autoDexFieldLimit=0xFFD0;//dex field must less than 65536,but field stat always less then in
	protected int autoDexMethodLimit=0xFFFF;//dex must less than 65536,55000 is more safer then 65535
	protected String autoDexMainDexOtherClasses=null;
	protected boolean aaptGenerateRFile=true;
	protected String apkPatchInputAllClassesJar=null;
	protected String apkPatchInputRTxt=null;
	protected String apkPatchBaseApk=null;
	protected String apkPatchResourceItemMapping=null;
	protected String proguardClasspath=null;
	protected String androidProjectDexTaskNodeInsertName=null;
	protected String multiAndroidProjectDexTaskNodeInsertName=null;
	
	protected Android android=null;
	protected final List<AndroidProject> androidProjectList=new CopyOnWriteArrayList<AndroidProject>();
//	protected final Map<String, AndroidProject> androidProjectMap=new ConcurrentHashMap<String, AndroidProject>();
	protected Map<String,Integer> androidProjectDexIdMap=new ConcurrentHashMap<String,Integer>();

	protected PublicAndroidProject publicAndroidProject=null;
	protected PublicAndroidProject publicRAndroidProject=null;
	protected PatchAndroidProject patchAndroidProject=null;
	protected AndroidProject mainAndroidProject=null;
	protected String mainAndroidApiJar=null;
	protected String mainAndroidProjectMainActivityName=null;
	protected List<String> autoDexMainDexOtherClassList=new CopyOnWriteArrayList<String>();
	protected Map<String,String> packageNameAndroidManifestMap=new ConcurrentHashMap<String,String>();

	private Map<TaskNodeInsertBean,Entry<Integer,List<AndroidProject>>> taskNodeInsertBeanDexEntryMap=new HashMap<TaskNodeInsertBean,Entry<Integer,List<AndroidProject>>>();
	//temporary
	private boolean allResourceFileHasCache=false;
	private boolean allAssetsFileHasCache=false;

	public static class Environment{
		public static final String ANDROID_HOME="ANDROID_HOME";
	}

	protected void initialize() {
		super.initialize();
		if(StringUtil.isNotBlank(this.projectMainProperties)){
			this.projectMainProperties=new File(this.projectMainProperties.trim()).getAbsolutePath();
		}
		if(StringUtil.isNotBlank(this.buildOutput)){
			this.buildOutput=this.buildOutput.trim();
		}else{
			File file=new File(StringUtil.BLANK);
			this.buildOutput=file.getAbsolutePath()+"/builder-gen";
		}
		if(StringUtil.isNotBlank(this.autoDexMainDexOtherClasses)){
			String[] classArray=this.autoDexMainDexOtherClasses.split(Constant.Symbol.COMMA);
			if(classArray!=null){
				for(String clazz:classArray){
					this.autoDexMainDexOtherClassList.add(clazz.trim());
				}
			}
		}

		if(StringUtil.isNotBlank(this.apkPatchInputRTxt)){
			this.apkPatchInputRTxt=new File(this.apkPatchInputRTxt.trim()).getAbsolutePath();
		}
		if(StringUtil.isNotBlank(this.apkPatchBaseApk)){
			this.apkPatchBaseApk=new File(this.apkPatchBaseApk.trim()).getAbsolutePath();
		}
		if(StringUtil.isNotBlank(this.apkPatchResourceItemMapping)){
			this.apkPatchResourceItemMapping=new File(this.apkPatchResourceItemMapping.trim()).getAbsolutePath();
		}
	
		if(StringUtil.isBlank(this.androidSdk)){
			if(this.builderConfiguration.getEnvironmentMap().containsKey(Environment.ANDROID_HOME)){
				this.androidSdk=this.builderConfiguration.getEnvironmentMap().get(Environment.ANDROID_HOME);
			}else{
				throw new ConfigurationException("Need to configurate "+Environment.ANDROID_HOME+" or set the android.sdk in build properties");
			}
		}else{
			this.androidSdk=this.androidSdk.trim();
		}
		
		if(StringUtil.isNotBlank(this.androidBuildToolsVersion)){
			this.androidBuildToolsVersion=this.androidBuildToolsVersion.trim();
		}
		this.android=new Android(this.androidSdk,this.androidBuildToolsVersion);

		logger.info(MAP_KEY_NEED_TO_CLEAN+Constant.Symbol.COLON+this.needToClean);
		logger.info(MAP_KEY_ANDROID_SDK+Constant.Symbol.COLON+this.androidSdk);
		logger.info(MAP_KEY_ANDROID_BUILD_TOOLS_VERSION+Constant.Symbol.COLON+this.android.getBuildTools());
		logger.info(MAP_KEY_PROJECT_MAIN_PROPERTIES+Constant.Symbol.COLON+this.projectMainProperties);
		logger.info(MAP_KEY_BUILD_OUTPUT+Constant.Symbol.COLON+this.buildOutput);
		logger.info(MAP_KEY_BUILD_OUTPUT_ECLIPSE+Constant.Symbol.COLON+this.buildOutputEclipse);
		logger.info(MAP_KEY_APK_DEBUG+Constant.Symbol.COLON+this.apkDebug);
		logger.info(MAP_KEY_APK_PRE_RELEASE+Constant.Symbol.COLON+this.apkPreRelease);
		logger.info(MAP_KEY_AUTO_DEX+Constant.Symbol.COLON+this.autoDex);
		logger.info(MAP_KEY_AUTO_DEX_LINEAR_ALLOC_LIMIT+Constant.Symbol.COLON+this.autoDexLinearAllocLimit);
		logger.info(MAP_KEY_AUTO_DEX_FIELD_LIMIT+Constant.Symbol.COLON+this.autoDexFieldLimit);
		logger.info(MAP_KEY_AUTO_DEX_METHOD_LIMIT+Constant.Symbol.COLON+this.autoDexMethodLimit);
		logger.info(MAP_KEY_AUTO_DEX_MAIN_DEX_OTHER_CLASSES+Constant.Symbol.COLON+this.autoDexMainDexOtherClasses);
		logger.info(MAP_KEY_APK_PATCH_INPUT_ALL_CLASSES_JAR+Constant.Symbol.COLON+this.apkPatchInputAllClassesJar);
	}

	protected void initializeAllProject(){
		super.initializeAllProject();
		for(Project project:this.projectList){
			AndroidProject androidProject=null;
			if(!(project instanceof AndroidProject)){
				continue;
			}
			androidProject=(AndroidProject)project;
			this.androidProjectList.add(androidProject);
//			if(!this.androidProjectMap.containsKey(androidProject.getName())){
//				this.androidProjectMap.put(androidProject.getName(),androidProject);
//			}
		}
		//initialize main android project
		Project mainProject=this.projectMap.get(this.projectMain);
		if(mainProject!=null&&mainProject instanceof Project){
			this.mainAndroidProject=(AndroidProject)mainProject;
		}
		String compileTarget=this.mainAndroidProject.getCompileTarget();
		this.mainAndroidApiJar=this.android.findAndroidApiJar(compileTarget);
		this.parseMainAndroidProjectAndroidManifest();
		this.publicAndroidProject=new PublicAndroidProject(this.mainAndroidProject.getOutputHome(),PublicAndroidProject.PUBLIC);
		this.publicRAndroidProject=new PublicAndroidProject(this.mainAndroidProject.getOutputHome(),PublicAndroidProject.PUBLIC_R);
		this.patchAndroidProject=new PatchAndroidProject(this.mainAndroidProject.getOutputHome(),this.apkDebug);
		//all android project,find parent android project list of android project and find all package name
		for(AndroidProject androidProject:this.androidProjectList){
			androidProject.setParentProjectList(this.findParentProjectList(androidProject));
			List<String> compileClasspathList=this.getAndroidProjectCompileClasspathList(androidProject);
			androidProject.setCompileClasspathList(compileClasspathList);
			List<String> compileSourceDirectoryList=this.getAndroidProjectCompileSourceDirectoryList(androidProject);
			androidProject.setCompileSourceDirectoryList(compileSourceDirectoryList);
			logger.info(androidProject);
			
			String packageName=androidProject.getPackageName();
			List<String> androidManifestList=androidProject.getAndroidManifestList();
			for(String androidManifest:androidManifestList){
				if(FileUtil.isExist(androidManifest)){
					if(!this.packageNameAndroidManifestMap.containsKey(packageName)){
						this.packageNameAndroidManifestMap.put(packageName, androidManifest);
					}
				}
			}
		}
	}

	/**
	 * increase handler bean list
	 */
	protected List<HandlerBean> increaseHandlerBeanList() {
		return null;
	}

	/**
	 * increase task node insert bean list
	 */
	protected List<TaskNodeInsertBean> increaseTaskNodeInsertBeanList() {
		List<TaskNodeInsertBean> taskNodeInsertBeanList=super.increaseTaskNodeInsertBeanList();
		//android project task node insert name
		TaskNodeInsertBean androidProjectTaskNodeInsertBean=this.builderConfiguration.getTaskNodeInsertBeanMap().get(this.projectTaskNodeInsertName);
		androidProjectTaskNodeInsertBean.setSkip(true);
		TaskNodeInsertBean androidProjectDexParentTaskNodeInsertBean=this.builderConfiguration.getTaskNodeInsertBeanMap().get(this.androidProjectDexTaskNodeInsertName);;
		if(androidProjectDexParentTaskNodeInsertBean!=null){
			androidProjectDexParentTaskNodeInsertBean.setSkip(true);
		}
		TaskNodeInsertBean multiDexParentTaskNodeInsertBean=this.builderConfiguration.getTaskNodeInsertBeanMap().get(this.multiAndroidProjectDexTaskNodeInsertName);;
		if(multiDexParentTaskNodeInsertBean!=null){
			multiDexParentTaskNodeInsertBean.setSkip(true);
		}
		List<String> dexTaskNodeNameList=new ArrayList<String>();
		for(Project project:this.projectList){
			if(androidProjectDexParentTaskNodeInsertBean!=null){
				//dex task node insert
				String dexTaskNodeName="dex_"+project.getName();
				String[] dexParentNames=androidProjectDexParentTaskNodeInsertBean.getParentNames();
				TaskNodeInsertBean dexTaskNodeInsertBean=new TaskNodeInsertBean();
				dexTaskNodeInsertBean.setName(dexTaskNodeName);
				dexTaskNodeInsertBean.setParentNames(dexParentNames);
				dexTaskNodeInsertBean.setHandlerName(androidProjectDexParentTaskNodeInsertBean.getHandlerName());
				taskNodeInsertBeanList.add(dexTaskNodeInsertBean);
				this.taskNodeInsertBeanProjectMap.put(dexTaskNodeInsertBean, project);
				dexTaskNodeNameList.add(dexTaskNodeName);
			}
		}
		//android dex task node parent change
		if(androidProjectDexParentTaskNodeInsertBean!=null){
			List<TaskNodeInsertBean> childTaskNodeInsertBeanList=this.builderConfiguration.getChildTaskNodeInsertBeanMap().get(this.androidProjectDexTaskNodeInsertName);
			if(childTaskNodeInsertBeanList!=null){
				for(TaskNodeInsertBean childTaskNodeInsertBean:childTaskNodeInsertBeanList){
					Set<String> parentNameSet=this.filterTaskNodeParentNames(childTaskNodeInsertBean.getParentNames(), this.androidProjectDexTaskNodeInsertName);
					parentNameSet.addAll(dexTaskNodeNameList);
					childTaskNodeInsertBean.setParentNames(parentNameSet.toArray(new String[]{}));
				}
			}
		}
		//merge dex task node insert
		if(multiDexParentTaskNodeInsertBean!=null){
			Map<Integer,List<AndroidProject>> dexMap=new HashMap<Integer,List<AndroidProject>>();
			for(AndroidProject androidProject:this.androidProjectList){
				List<AndroidProject> list=null;
				if(dexMap.containsKey(androidProject.getDexId())){
					list=dexMap.get(androidProject.getDexId());
				}else{
					list=new ArrayList<AndroidProject>();
					dexMap.put(androidProject.getDexId(), list);
				}
				list.add(androidProject);
			}
			List<String> multiDexTaskNodeNameList=new ArrayList<String>();
			Iterator<Entry<Integer,List<AndroidProject>>> iterator=dexMap.entrySet().iterator();
			while(iterator.hasNext()){
				final Entry<Integer,List<AndroidProject>> entry=iterator.next();
				final Integer dexId=entry.getKey();
				String multiDexTaskNodeName="multi_dex_"+dexId;
				String[] multiDexParentNames=multiDexParentTaskNodeInsertBean.getParentNames();
				TaskNodeInsertBean multiDexTaskNodeInsertBean=new TaskNodeInsertBean();
				multiDexTaskNodeInsertBean.setName(multiDexTaskNodeName);
				multiDexTaskNodeInsertBean.setParentNames(multiDexParentNames);
				multiDexTaskNodeInsertBean.setHandlerName(multiDexParentTaskNodeInsertBean.getHandlerName());
				taskNodeInsertBeanList.add(multiDexTaskNodeInsertBean);
				this.taskNodeInsertBeanDexEntryMap.put(multiDexTaskNodeInsertBean, entry);
				multiDexTaskNodeNameList.add(multiDexTaskNodeName);
			}
			List<TaskNodeInsertBean> childTaskNodeInsertBeanList=this.builderConfiguration.getChildTaskNodeInsertBeanMap().get(this.multiAndroidProjectDexTaskNodeInsertName);
			if(childTaskNodeInsertBeanList!=null){
				for(TaskNodeInsertBean childTaskNodeInsertBean:childTaskNodeInsertBeanList){
					Set<String> parentNameSet=this.filterTaskNodeParentNames(childTaskNodeInsertBean.getParentNames(), this.multiAndroidProjectDexTaskNodeInsertName);
					parentNameSet.addAll(multiDexTaskNodeNameList);
					childTaskNodeInsertBean.setParentNames(parentNameSet.toArray(new String[]{}));
				}
			}
		}
		return taskNodeInsertBeanList;
	}

	protected void initializingHandlerBean(HandlerBean handlerBean) {}

	protected void initializingTaskNodeInsertBean(TaskNodeInsertBean taskNodeInsertBean) {
		this.renameTaskNodeInsertBean(taskNodeInsertBean);
		Handler handler=taskNodeInsertBean.getHandlerInstance();
		if(handler instanceof AndroidProjectHandler){
			AndroidProjectHandler androidProjectHandler=(AndroidProjectHandler)handler;
			Project project=this.taskNodeInsertBeanProjectMap.get(taskNodeInsertBean);
			if(project!=null&&project instanceof AndroidProject){
				AndroidProject androidProject=(AndroidProject)project;
				androidProjectHandler.setAndroidProject(androidProject);
			}
		}else if(handler instanceof MultiAndroidProjectDexHandler){
			MultiAndroidProjectDexHandler multiAndroidProjectDexHandler=(MultiAndroidProjectDexHandler)handler;
			Entry<Integer,List<AndroidProject>> entry=this.taskNodeInsertBeanDexEntryMap.get(taskNodeInsertBean);
			if(entry!=null){
				multiAndroidProjectDexHandler.setDexId(entry.getKey());
				multiAndroidProjectDexHandler.setAndroidProjectList(entry.getValue());
			}
		}
	}

	/**
	 * @return the needToClean
	 */
	public boolean isNeedToClean() {
		return needToClean;
	}
	/**
	 * @param needToClean the needToClean to set
	 */
	public void setNeedToClean(boolean needToClean) {
		this.needToClean = needToClean;
	}
	/**
	 * @return the androidSdk
	 */
	public String getAndroidSdk() {
		return androidSdk;
	}
	/**
	 * @param androidSdk the androidSdk to set
	 */
	public void setAndroidSdk(String androidSdk) {
		this.androidSdk = androidSdk;
	}
	/**
	 * @return the androidBuildToolsVersion
	 */
	public String getAndroidBuildToolsVersion() {
		return androidBuildToolsVersion;
	}
	/**
	 * @param androidBuildToolsVersion the androidBuildToolsVersion to set
	 */
	public void setAndroidBuildToolsVersion(String androidBuildToolsVersion) {
		this.androidBuildToolsVersion = androidBuildToolsVersion;
	}
	/**
	 * @return the projectMainProperties
	 */
	public String getProjectMainProperties() {
		return projectMainProperties;
	}
	/**
	 * @param projectMainProperties the projectMainProperties to set
	 */
	public void setProjectMainProperties(String projectMainProperties) {
		this.projectMainProperties = projectMainProperties;
	}
	/**
	 * @return the projectLoadModeAttachBaseContextMultiDex
	 */
	public boolean isProjectLoadModeAttachBaseContextMultiDex() {
		return projectLoadModeAttachBaseContextMultiDex;
	}
	/**
	 * @param projectLoadModeAttachBaseContextMultiDex the projectLoadModeAttachBaseContextMultiDex to set
	 */
	public void setProjectLoadModeAttachBaseContextMultiDex(boolean projectLoadModeAttachBaseContextMultiDex) {
		this.projectLoadModeAttachBaseContextMultiDex = projectLoadModeAttachBaseContextMultiDex;
	}
	/**
	 * @return the buildOutputEclipse
	 */
	public boolean isBuildOutputEclipse() {
		return buildOutputEclipse;
	}
	/**
	 * @param buildOutputEclipse the buildOutputEclipse to set
	 */
	public void setBuildOutputEclipse(boolean buildOutputEclipse) {
		this.buildOutputEclipse = buildOutputEclipse;
	}
	/**
	 * @return the apkDebug
	 */
	public boolean isApkDebug() {
		return apkDebug;
	}
	/**
	 * @param apkDebug the apkDebug to set
	 */
	public void setApkDebug(boolean apkDebug) {
		this.apkDebug = apkDebug;
	}
	/**
	 * @return the apkPreRelease
	 */
	public boolean isApkPreRelease() {
		return apkPreRelease;
	}
	/**
	 * @param apkPreRelease the apkPreRelease to set
	 */
	public void setApkPreRelease(boolean apkPreRelease) {
		this.apkPreRelease = apkPreRelease;
	}
	/**
	 * @return the autoDex
	 */
	public boolean isAutoDex() {
		return autoDex;
	}
	/**
	 * @param autoDex the autoDex to set
	 */
	public void setAutoDex(boolean autoDex) {
		this.autoDex = autoDex;
	}
	/**
	 * @return the autoDexLinearAllocLimit
	 */
	public int getAutoDexLinearAllocLimit() {
		return autoDexLinearAllocLimit;
	}
	/**
	 * @param autoDexLinearAllocLimit the autoDexLinearAllocLimit to set
	 */
	public void setAutoDexLinearAllocLimit(int autoDexLinearAllocLimit) {
		this.autoDexLinearAllocLimit = autoDexLinearAllocLimit;
	}
	/**
	 * @return the autoDexFieldLimit
	 */
	public int getAutoDexFieldLimit() {
		return autoDexFieldLimit;
	}
	/**
	 * @param autoDexFieldLimit the autoDexFieldLimit to set
	 */
	public void setAutoDexFieldLimit(int autoDexFieldLimit) {
		this.autoDexFieldLimit = autoDexFieldLimit;
	}
	/**
	 * @return the autoDexMethodLimit
	 */
	public int getAutoDexMethodLimit() {
		return autoDexMethodLimit;
	}
	/**
	 * @param autoDexMethodLimit the autoDexMethodLimit to set
	 */
	public void setAutoDexMethodLimit(int autoDexMethodLimit) {
		this.autoDexMethodLimit = autoDexMethodLimit;
	}
	/**
	 * @return the autoDexMainDexOtherClasses
	 */
	public String getAutoDexMainDexOtherClasses() {
		return autoDexMainDexOtherClasses;
	}
	/**
	 * @param autoDexMainDexOtherClasses the autoDexMainDexOtherClasses to set
	 */
	public void setAutoDexMainDexOtherClasses(String autoDexMainDexOtherClasses) {
		this.autoDexMainDexOtherClasses = autoDexMainDexOtherClasses;
	}
	/**
	 * @return the aaptGenerateRFile
	 */
	public boolean isAaptGenerateRFile() {
		return aaptGenerateRFile;
	}
	/**
	 * @param aaptGenerateRFile the aaptGenerateRFile to set
	 */
	public void setAaptGenerateRFile(boolean aaptGenerateRFile) {
		this.aaptGenerateRFile = aaptGenerateRFile;
	}
	/**
	 * @return the apkPatchInputAllClassesJar
	 */
	public String getApkPatchInputAllClassesJar() {
		return apkPatchInputAllClassesJar;
	}
	/**
	 * @param apkPatchInputAllClassesJar the apkPatchInputAllClassesJar to set
	 */
	public void setApkPatchInputAllClassesJar(String apkPatchInputAllClassesJar) {
		this.apkPatchInputAllClassesJar = apkPatchInputAllClassesJar;
	}
	/**
	 * @return the androidProjectList
	 */
	public List<AndroidProject> getAndroidProjectList() {
		return androidProjectList;
	}
	/**
	 * get public android project
	 * @return PublicAndroidProject
	 */
	public PublicAndroidProject getPublicAndroidProject(){
		return this.publicAndroidProject;
	}

	/**
	 * @return the publicRAndroidProject
	 */
	public PublicAndroidProject getPublicRAndroidProject() {
		return publicRAndroidProject;
	}

	/**
	 * get main android project
	 * @return AndroidProject
	 */
	public AndroidProject getMainAndroidProject(){
		return this.mainAndroidProject;
	}

	/**
	 * @return the mainAndroidApiJar
	 */
	public String getMainAndroidApiJar() {
		return mainAndroidApiJar;
	}

	/**
	 * @return the mainAndroidProjectMainActivityName
	 */
	public String getMainAndroidProjectMainActivityName() {
		return mainAndroidProjectMainActivityName;
	}

	/**
	 * @return the autoDexMainDexOtherClassList
	 */
	public List<String> getAutoDexMainDexOtherClassList() {
		return autoDexMainDexOtherClassList;
	}

	/**
	 * @return the packageNameAndroidManifestMap
	 */
	public Map<String, String> getPackageNameAndroidManifestMap() {
		return packageNameAndroidManifestMap;
	}

	/**
	 * @return the android
	 */
	public Android getAndroid() {
		return android;
	}

	/**
	 * find directory of android project list
	 * @param androidProjectList
	 * @param directoryType
	 * @return List<String>
	 */
	public List<String> findDirectoryOfAndroidProjectList(List<AndroidProject> androidProjectList,AndroidProject.DirectoryType directoryType){
		List<String> directoryList=new ArrayList<String>();
		for(AndroidProject androidProject:androidProjectList){
			String directory=null;
			switch(directoryType){
			case SRC:
				directoryList.addAll(androidProject.getSourceDirectoryList());
				break;
			case RES:
				directoryList.addAll(androidProject.getResourceDirectoryList());
				break;
			case GEN:
				directory=androidProject.getGen();
				break;
			case LIBS:
				directoryList.addAll(androidProject.getLibsDirectoryList());
				break;
			case ASSETS:
				directoryList.addAll(androidProject.getAssetsDirectoryList());
				break;
			case CLASSES_OUTPUT:
				directory=androidProject.getClassesOutput();
				break;
			case GEN_OUTPUT:
				directory=androidProject.getGenOutput();
				break;
			case ANDROID_MANIFEST:
				directoryList.addAll(androidProject.getAndroidManifestList());
				break;
			}
			if(directory!=null&&FileUtil.isExist(directory)){
				directoryList.add(directory);
			}
		}
		return directoryList;
	}

	/**
	 * find max google api target in all android project
	 * @return String
	 */
	public String findMaxGoogleApiTargetInAllAndroidProject(){
		String target=null;
		int level=0;
		for(AndroidProject androidProject:this.androidProjectList){
			String compileTarget=androidProject.getCompileTarget();
			if(this.android.isGoogleApi(compileTarget)){
				GoogleApi googleApi=this.android.findGoogleApi(compileTarget);
				if(googleApi!=null&&googleApi.getApiLevel()>level){
					level=googleApi.getApiLevel();
					target=googleApi.getTarget();
				}
			}
		}
		return target;
	}

	/**
	 * find max android api target in all android project
	 * @return String
	 */
	public String findMaxAndroidApiTargetInAllAndroidProject(){
		String target=null;
		int level=0;
		for(AndroidProject androidProject:this.androidProjectList){
			String compileTarget=androidProject.getCompileTarget();
			if(this.android.isAndroidApi(compileTarget)){
				AndroidApi androidApi=this.android.findAndroidApi(compileTarget);
				if(androidApi!=null&&androidApi.getApiLevel()>level){
					level=androidApi.getApiLevel();
					target=androidApi.getTarget();
				}
			}
		}
		return target;
	}

	/**
	 * get main android project package name from android manifest
	 * @return String
	 */
	private void parseMainAndroidProjectAndroidManifest(){
		if(!this.mainAndroidProject.getAndroidManifestList().isEmpty()){
			String mainProjectAndroidManifest=this.mainAndroidProject.getAndroidManifestList().get(0);
			try {
				Document document=JavaXmlUtil.parse(mainProjectAndroidManifest);
				if(document!=null){
					XPathFactory factory = XPathFactory.newInstance();
					XPath xpath = factory.newXPath();
					XPathExpression expression = xpath.compile("/manifest/application/activity");
					NodeList nodeList = (NodeList) expression.evaluate(document, XPathConstants.NODESET);
					if(nodeList!=null){
						for(int i=0;i<nodeList.getLength();i++){
							Node node=nodeList.item(i);
							String activityName=node.getAttributes().getNamedItem("android:name").getTextContent();
							Element element=(Element)node;
							NodeList actionNodeList=element.getElementsByTagName("action");
							for(int j=0;j<actionNodeList.getLength();j++){
								Node activityActionNode=actionNodeList.item(j).getAttributes().getNamedItem("android:name");
								if(activityActionNode!=null){
									String activityActionName=activityActionNode.getTextContent();
									if(activityActionName.equals("android.intent.action.MAIN")){
										if(activityName.startsWith(Constant.Symbol.DOT)){
											this.mainAndroidProjectMainActivityName=activityName;
										}else{
											this.mainAndroidProjectMainActivityName=activityName.replace(this.mainAndroidProject.getPackageName(),StringUtil.BLANK);
										}
										break;
									}
								}
							}
						}
					}
				}
			} catch (Exception e) {
				throw new BuildException(e);
			}
		}
	}

	/**
	 * get android project compile classpath list
	 * @param androidProject
	 * @return List<String>
	 */
	private List<String> getAndroidProjectCompileClasspathList(AndroidProject androidProject){
		List<String> classpathList=this.getProjectCompileClasspathList(androidProject);
		classpathList.add(this.android.getAnnotationJar());

		String publicClassesOutput=this.publicAndroidProject.getClassesOutput();
		classpathList.add(publicClassesOutput);
		if(this.isAaptGenerateRFile()){
			String publicRClassesOutput=this.publicRAndroidProject.getClassesOutput();
			classpathList.add(publicRClassesOutput);
		}
		if(StringUtil.isBlank(androidProject.getCompileTarget())){
			classpathList.add(this.mainAndroidApiJar);
		}else{
			String compileTarget=androidProject.getCompileTarget();
			try{
				classpathList.addAll(this.android.findApiJarList(compileTarget));
			}catch (NullPointerException e) {
				throw new NullPointerException(androidProject.getName()+","+e.getMessage());
			}
		}
		return classpathList;
	}

	/**
	 * get android project compile source directory list
	 * @param androidProject
	 * @return List<String>
	 */
	private List<String> getAndroidProjectCompileSourceDirectoryList(AndroidProject androidProject){
		List<String> sourceList=new ArrayList<String>();
		List<String> sourceDirectoryList=androidProject.getSourceDirectoryList();//getSources();
		if(sourceDirectoryList!=null){
			for(String sourceDirectory:sourceDirectoryList){
//				projectSource=androidProject.getHome()+"/"+projectSource;
				sourceList.add(sourceDirectory);
			}
		}
		sourceList.add(androidProject.getGenOutput());
		return sourceList;
	}

	/**
	 * @return the allResourceFileHasCache
	 */
	public boolean isAllResourceFileHasCache() {
		return allResourceFileHasCache;
	}

	/**
	 * @param allResourceFileHasCache the allResourceFileHasCache to set
	 */
	public void setAllResourceFileHasCache(boolean allResourceFileHasCache) {
		this.allResourceFileHasCache = allResourceFileHasCache;
	}

	/**
	 * @return the allAssetsFileHasCache
	 */
	public boolean isAllAssetsFileHasCache() {
		return allAssetsFileHasCache;
	}

	/**
	 * @param allAssetsFileHasCache the allAssetsFileHasCache to set
	 */
	public void setAllAssetsFileHasCache(boolean allAssetsFileHasCache) {
		this.allAssetsFileHasCache = allAssetsFileHasCache;
	}

	/**
	 * @param androidProjectDexTaskNodeInsertName the androidProjectDexTaskNodeInsertName to set
	 */
	public void setAndroidProjectDexTaskNodeInsertName(String androidProjectDexTaskNodeInsertName) {
		this.androidProjectDexTaskNodeInsertName = androidProjectDexTaskNodeInsertName;
	}

	/**
	 * @param multiAndroidProjectDexTaskNodeInsertName the multiAndroidProjectDexTaskNodeInsertName to set
	 */
	public void setMultiAndroidProjectDexTaskNodeInsertName(String multiAndroidProjectDexTaskNodeInsertName) {
		this.multiAndroidProjectDexTaskNodeInsertName = multiAndroidProjectDexTaskNodeInsertName;
	}

	/**
	 * @return the apkPatchInputRTxt
	 */
	public String getApkPatchInputRTxt() {
		return apkPatchInputRTxt;
	}

	/**
	 * @param apkPatchInputRTxt the apkPatchInputRTxt to set
	 */
	public void setApkPatchInputRTxt(String apkPatchInputRTxt) {
		this.apkPatchInputRTxt = apkPatchInputRTxt;
	}

	/**
	 * @return the apkPatchBaseApk
	 */
	public String getApkPatchBaseApk() {
		return apkPatchBaseApk;
	}

	/**
	 * @param apkPatchBaseApk the apkPatchBaseApk to set
	 */
	public void setApkPatchBaseApk(String apkPatchBaseApk) {
		this.apkPatchBaseApk = apkPatchBaseApk;
	}

	/**
	 * @return the apkPatchResourceItemMapping
	 */
	public String getApkPatchResourceItemMapping() {
		return apkPatchResourceItemMapping;
	}

	/**
	 * @param apkPatchResourceItemMapping the apkPatchResourceItemMapping to set
	 */
	public void setApkPatchResourceItemMapping(String apkPatchResourceItemMapping) {
		this.apkPatchResourceItemMapping = apkPatchResourceItemMapping;
	}

	/**
	 * @return the proguardClasspath
	 */
	public String getProguardClasspath() {
		return proguardClasspath;
	}

	/**
	 * @param proguardClasspath the proguardClasspath to set
	 */
	public void setProguardClasspath(String proguardClasspath) {
		this.proguardClasspath = proguardClasspath;
	}

	/**
	 * @return the patchAndroidProject
	 */
	public PatchAndroidProject getPatchAndroidProject() {
		return patchAndroidProject;
	}
}
