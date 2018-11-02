package com.oneliang.tools.builder.android.base;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import com.oneliang.Constants;
import com.oneliang.tools.builder.base.BuilderUtil;
import com.oneliang.util.common.StringUtil;
import com.oneliang.util.file.FileUtil;
import com.oneliang.util.logging.Logger;
import com.oneliang.util.logging.LoggerManager;

public class Android {

	private static final Logger logger=LoggerManager.getLogger(AndroidConfiguration.class);

	private static final String ANDROID_JAR="android.jar";
	private static final String ANNOTATIONS_JAR="annotations.jar";
	private static final String SDKLIB_JAR="sdklib.jar";
	private static final String FRAMEWORK_AIDL="framework.aidl";
	private static final String GOOGLE_API_LIBS="libs";
	private static final String BUILD_TOOLS="build-tools";
	private static final String ADD_ONS="add-ons";
	private static final String PLATFORMS="platforms";
	private static final String PLATFORM_TOOLS="platform-tools";
	private static final String TOOLS="tools";
	private static final String PROGUARD="proguard";
	private static final String PROGUARD_JAR="proguard.jar";
	private static final String PROGUARD_ANDROID_TXT="proguard-android.txt";
	private static final String PROGUARD_ANDROID_OPTIMIZE_TXT="proguard-android-optimize.txt";
	private static final String AAPT="aapt";
	private static final String AIDL="aidl";
	private static final String DX="dx";
	private static final String ZIP_ALIGN="zipalign";
	private static final String ADB="adb";
	private String home=null;
	private String buildTools=null;
	private String addOns=null;
	private String platforms=null;
	private String platformTools=null;
	private String annotationJar=null;
	private String tools=null;
	private String proguardJar=null;
	private String proguardAndroidTxt=null;
	private String proguardAndroidOptimizeTxt=null;
	private String aaptExecutor=null;
	private String aidlExecutor=null;
	private String dx=null;
	private String zipAlignExecutor=null;
	private String sdkLibJar=null;
	private String adbExecutor=null;
	private BuildTool buildTool=null;
	private Map<String,GoogleApi> googleApiMap=new ConcurrentHashMap<String, GoogleApi>();
	private Map<String,AndroidApi> androidApiMap=new ConcurrentHashMap<String, AndroidApi>();
	private Map<Integer,AndroidApi> androidApiLevelMap=new ConcurrentHashMap<Integer, AndroidApi>();

	public Android(String home,String buildToolsVersion){
		if(home==null){
			throw new NullPointerException("home is null");
		}
		this.home=home;
		File file=new File(this.home);
		this.home=file.getAbsolutePath();
		String osExecuteFileSuffix=BuilderUtil.isWindowsOS()?(Constants.Symbol.DOT+Constants.File.EXE):StringUtil.BLANK;

		if(StringUtil.isBlank(buildToolsVersion)){
			String buildToolsDirectory=this.home+"/"+BUILD_TOOLS;
			File buildToolsFile=new File(buildToolsDirectory);
			File[] buildToolsDirectorys=buildToolsFile.listFiles();
			if(buildToolsDirectorys!=null&&buildToolsDirectorys.length>0){
				int maxBuildToolVersion=0;
				for(File buildToolDirectory:buildToolsDirectorys){
					if(buildToolDirectory.isDirectory()){
						String buildToolDirectoryPath=buildToolDirectory.getAbsolutePath();
						String sourceProperties=buildToolDirectoryPath+"/"+AndroidApi.SOURCE_PROPERTIES;
						try {
							Properties properties = FileUtil.getProperties(sourceProperties);
							BuildTool buildTool=new BuildTool(properties);
							buildTool.setDirectory(buildToolDirectoryPath);
							if(buildTool.getPkgRevisionInt()>maxBuildToolVersion){
								maxBuildToolVersion=buildTool.getPkgRevisionInt();
								this.buildTool=buildTool;
							}
						} catch (Exception e) {
							logger.warning("It is not a build tools directory:"+sourceProperties);
						}
					}
				}
			}else{
				throw new AndroidException("No suitable build-tools of android sdk,path:"+buildToolsDirectory);
			}
			this.buildTools=this.buildTool.getDirectory();
		}else{
			this.buildTools=this.home+"/"+BUILD_TOOLS+"/"+buildToolsVersion;
		}
		this.addOns=this.home+"/"+ADD_ONS;
		this.platforms=this.home+"/"+PLATFORMS;
		this.platformTools=this.home+"/"+PLATFORM_TOOLS;
		this.tools=this.home+"/"+TOOLS;
		this.annotationJar=this.tools+"/support/"+ANNOTATIONS_JAR;
		this.aaptExecutor=this.buildTools+"/"+AAPT+osExecuteFileSuffix;
		this.aidlExecutor=this.buildTools+"/"+AIDL+osExecuteFileSuffix;
		this.dx=this.buildTools+"/"+DX+(BuilderUtil.isWindowsOS()?".bat":StringUtil.BLANK);
		this.zipAlignExecutor=this.buildTools+"/"+ZIP_ALIGN+osExecuteFileSuffix;
		if(!FileUtil.isExist(this.zipAlignExecutor)){
			this.zipAlignExecutor=this.tools+"/"+ZIP_ALIGN+osExecuteFileSuffix;
		}
		this.sdkLibJar=this.tools+"/lib/"+SDKLIB_JAR;
		this.proguardJar=this.tools+"/"+PROGUARD+"/lib/"+PROGUARD_JAR;
		this.proguardAndroidTxt=this.tools+"/"+PROGUARD+"/"+PROGUARD_ANDROID_TXT;
		this.proguardAndroidOptimizeTxt=this.tools+"/"+PROGUARD+"/"+PROGUARD_ANDROID_OPTIMIZE_TXT;
		this.adbExecutor=this.platformTools+"/"+ADB+osExecuteFileSuffix;

		File addOnsFile=new File(this.addOns);
		File[] googleApiDirectorys=addOnsFile.listFiles();
		if(googleApiDirectorys!=null){
			for(File googleApiDirectory:googleApiDirectorys){
				if(googleApiDirectory.isDirectory()){
					String googleApiDirectoryPath=googleApiDirectory.getAbsolutePath();
					String sourceProperties=googleApiDirectoryPath+"/"+GoogleApi.MANIFEST_INI;
					try {
						Properties properties = FileUtil.getProperties(sourceProperties);
						GoogleApi googleApi=new GoogleApi(properties);
						googleApi.setDirectory(googleApiDirectoryPath);
						String googleApiLibs=googleApi.getDirectory()+"/"+GOOGLE_API_LIBS;
						FileUtil.MatchOption matchOption=new FileUtil.MatchOption(googleApiLibs);
						matchOption.fileSuffix=Constants.Symbol.DOT+Constants.File.JAR;
						googleApi.setJarList(FileUtil.findMatchFile(matchOption));
						this.googleApiMap.put(googleApi.getTarget(), googleApi);
					} catch (Exception e) {
						logger.warning("It is not a google api directory:"+sourceProperties);
					}
				}
			}
		}
		File platformsFile=new File(this.platforms);
		File[] androidApiDirectorys=platformsFile.listFiles();
		if(androidApiDirectorys!=null){
			for(File androidApiDirectory:androidApiDirectorys){
				if(androidApiDirectory.isDirectory()){
					String androidApiDirectoryPath=androidApiDirectory.getAbsolutePath();
					String sourceProperties=androidApiDirectoryPath+"/"+AndroidApi.SOURCE_PROPERTIES;
					if(FileUtil.isExist(sourceProperties)){
						Properties properties = FileUtil.getProperties(sourceProperties);
						AndroidApi androidApi=new AndroidApi(properties);
						androidApi.setDirectory(androidApiDirectoryPath);
						String androidJar=androidApi.getDirectory()+"/"+ANDROID_JAR;
						androidApi.setJarList(Arrays.asList(androidJar));
						this.androidApiMap.put(androidApi.getTarget(), androidApi);
						if(this.androidApiLevelMap.containsKey(androidApi.getApiLevel())){
							AndroidApi oldAndroidApi=this.androidApiLevelMap.get(androidApi.getApiLevel());
							if(androidApi.getPkgRevision()>oldAndroidApi.getPkgRevision()){
								this.androidApiLevelMap.put(androidApi.getApiLevel(), androidApi);
								logger.verbose("Android api level replace,from:{"+androidApi.getApiLevel()+","+androidApi.getPkgRevision()+"},to:{"+oldAndroidApi.getApiLevel()+","+oldAndroidApi.getPkgRevision()+"}");
							}
						}else{
							this.androidApiLevelMap.put(androidApi.getApiLevel(), androidApi);
						}
					} else {
						logger.warning("It is not a android api directory:"+sourceProperties);
					}
				}
			}
		}
	}

	/**
	 * is android api
	 * @param target
	 * @return boolean
	 */
	public boolean isAndroidApi(String target){
		boolean result=false;
		if(this.androidApiMap.containsKey(target)){
			result=true;
		}
		return result;
	}

	/**
	 * find android api
	 * @param target
	 * @return AndroidApi
	 */
	public AndroidApi findAndroidApi(String target){
		AndroidApi androidApi=null;
		if(this.androidApiMap.containsKey(target)){
			androidApi=this.androidApiMap.get(target);
		}
		return androidApi;
	}

	/**
	 * is google api
	 * @param target
	 * @return boolean
	 */
	public boolean isGoogleApi(String target){
		boolean result=false;
		if(this.googleApiMap.containsKey(target)){
			result=true;
		}
		return result;
	}

	/**
	 * find google api
	 * @param target
	 * @return GoogleApi
	 */
	public GoogleApi findGoogleApi(String target){
		GoogleApi googleApi=null;
		if(this.googleApiMap.containsKey(target)){
			googleApi=this.googleApiMap.get(target);
		}
		return googleApi;
	}

	/**
	 * find android api jar
	 * @param target
	 * @return String
	 */
	public String findAndroidApiJar(String target){
		return this.findAndroidApiFile(target, ANDROID_JAR);
	}

	/**
	 * find android api framework aidl
	 * @param target
	 * @return String
	 */
	public String findAndroidApiFrameworkAidl(String target){
		return this.findAndroidApiFile(target, FRAMEWORK_AIDL);
	}

	/**
	 * find android api file
	 * @param target
	 * @param file
	 * @return String
	 */
	private String findAndroidApiFile(String target,String file){
		String result=null;
		if(this.androidApiMap.containsKey(target)){
			result=this.androidApiMap.get(target).getDirectory()+"/"+file;
		}else if(this.googleApiMap.containsKey(target)){
			GoogleApi googleApi=this.googleApiMap.get(target);
			AndroidApi androidApi=this.androidApiLevelMap.get(googleApi.getApiLevel());
			result=androidApi.getDirectory()+"/"+file;
		}else{
			throw new NullPointerException("can not find compile target:"+target);
		}
		return result;
	}

	/**
	 * find api jar list
	 * @param target
	 * @return boolean
	 */
	public List<String> findApiJarList(String target){
		List<String> jarList=new ArrayList<String>();
		if(this.androidApiMap.containsKey(target)){
			String androidJar=this.androidApiMap.get(target).getDirectory()+"/"+ANDROID_JAR;
			jarList.add(androidJar);
		}else if(this.googleApiMap.containsKey(target)){
			GoogleApi googleApi=this.googleApiMap.get(target);
			jarList.addAll(googleApi.getJarList());
			AndroidApi androidApi=this.androidApiLevelMap.get(googleApi.getApiLevel());
			jarList.addAll(androidApi.getJarList());
		}else{
			throw new NullPointerException("can not find compile target:"+target);
		}
		return jarList;
	}

	/**
	 * find google api jar list
	 * @param target
	 * @return boolean
	 */
	public List<String> findGoogleApiJarList(String target){
		List<String> jarList=new ArrayList<String>();
		if(this.googleApiMap.containsKey(target)){
			GoogleApi googleApi=this.googleApiMap.get(target);
			jarList.addAll(googleApi.getJarList());
		}else{
			throw new NullPointerException("can not find compile target:"+target);
		}
		return jarList;
	}

	/**
	 * @return the buildTools
	 */
	public String getBuildTools() {
		return buildTools;
	}

	/**
	 * @return the addOns
	 */
	public String getAddOns() {
		return addOns;
	}

	/**
	 * @return the platforms
	 */
	public String getPlatforms() {
		return platforms;
	}

	/**
	 * @return the platformTools
	 */
	public String getPlatformTools() {
		return platformTools;
	}

	/**
	 * @return the annotationJar
	 */
	public String getAnnotationJar() {
		return annotationJar;
	}

	/**
	 * @return the aaptExecutor
	 */
	public String getAaptExecutor() {
		return aaptExecutor;
	}

	/**
	 * @return the aidl
	 */
	public String getAidlExecutor() {
		return aidlExecutor;
	}

	/**
	 * @return the dx
	 */
	public String getDx() {
		return dx;
	}

	/**
	 * @return the sdkLibJar
	 */
	public String getSdkLibJar() {
		return sdkLibJar;
	}

	/**
	 * @return the adb
	 */
	public String getAdbExecutor() {
		return adbExecutor;
	}

	private static class AndroidException extends RuntimeException {
		private static final long serialVersionUID = 248281991262324811L;
		public AndroidException(String message) {
			super(message);
		}
		public AndroidException(Throwable cause) {
			super(cause);
		}
		public AndroidException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	/**
	 * @return the home
	 */
	public String getHome() {
		return home;
	}

	/**
	 * @return the proguardJar
	 */
	public String getProguardJar() {
		return proguardJar;
	}

	/**
	 * @return the zipAlignExecutor
	 */
	public String getZipAlignExecutor() {
		return zipAlignExecutor;
	}

	/**
	 * @return the proguardAndroidTxt
	 */
	public String getProguardAndroidTxt() {
		return proguardAndroidTxt;
	}

	/**
	 * @return the proguardAndroidOptimizeTxt
	 */
	public String getProguardAndroidOptimizeTxt() {
		return proguardAndroidOptimizeTxt;
	}
}
