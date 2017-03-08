package com.oneliang.tools.builder.android.base;

import java.util.List;
import java.util.Properties;

import com.oneliang.util.common.StringUtil;

public class AndroidApi {

	static final String SOURCE_PROPERTIES="source.properties";

	private static final String ANDROID_VERSION_API_LEVEL="AndroidVersion.ApiLevel";
	private static final String ANDROID_VERSION_CODE_NAME="AndroidVersion.CodeName";
	private static final String PKG_REVISION="Pkg.Revision";
	private static final String ANDROID="android-";

	private int apiLevel=0;
	private String codeName=null;
	private int pkgRevision=0;
	private String directory=null;
	private String target=null;
	private List<String> jarList=null;

	public AndroidApi(Properties properties){
		this.apiLevel=Integer.parseInt(properties.getProperty(ANDROID_VERSION_API_LEVEL, String.valueOf(0)).trim());
		this.codeName=properties.getProperty(ANDROID_VERSION_CODE_NAME);
		this.pkgRevision=Integer.parseInt(properties.getProperty(PKG_REVISION, String.valueOf(0)).trim());
		if(StringUtil.isNotBlank(this.codeName)){
			this.target=ANDROID+this.codeName;
		}else{
			this.target=ANDROID+this.apiLevel;
		}
	}

	/**
	 * @return the directory
	 */
	public String getDirectory() {
		return directory;
	}

	/**
	 * @param directory the directory to set
	 */
	void setDirectory(String directory) {
		this.directory = directory;
	}

	/**
	 * @return the apiLevel
	 */
	public int getApiLevel() {
		return apiLevel;
	}

	/**
	 * @param apiLevel the apiLevel to set
	 */
	void setApiLevel(int apiLevel) {
		this.apiLevel = apiLevel;
	}

	/**
	 * @return the target
	 */
	public String getTarget() {
		return target;
	}

	/**
	 * @return the jarList
	 */
	public List<String> getJarList() {
		return jarList;
	}

	/**
	 * @param jarList the jarList to set
	 */
	void setJarList(List<String> jarList) {
		this.jarList = jarList;
	}

	/**
	 * @return the codeName
	 */
	public String getCodeName() {
		return codeName;
	}

	/**
	 * @param codeName the codeName to set
	 */
	void setCodeName(String codeName) {
		this.codeName = codeName;
	}

	/**
	 * @return the pkgRevision
	 */
	public int getPkgRevision() {
		return pkgRevision;
	}

	/**
	 * @param pkgRevision the pkgRevision to set
	 */
	void setPkgRevision(int pkgRevision) {
		this.pkgRevision = pkgRevision;
	}
}
