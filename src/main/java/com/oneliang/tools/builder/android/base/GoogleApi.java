package com.oneliang.tools.builder.android.base;

import java.util.List;
import java.util.Properties;

import com.oneliang.Constant;
import com.oneliang.util.common.StringUtil;

public class GoogleApi {

	static final String MANIFEST_INI="manifest.ini";

	private static final String NAME="name";
	private static final String VENDOR="vendor";
	private static final String API="api";

	private String name=null;
	private String vendor=null;
	private int apiLevel=0;
	private String directory=null;
	private String target=null;
	private List<String> jarList=null;

	public GoogleApi(Properties properties){
		this.name=properties.getProperty(NAME, StringUtil.BLANK).trim();
		this.vendor=properties.getProperty(VENDOR, StringUtil.BLANK).trim();
		this.apiLevel=Integer.parseInt(properties.getProperty(API, String.valueOf(0)).trim());
		this.target=this.vendor+Constant.Symbol.COLON+this.name+Constant.Symbol.COLON+this.apiLevel;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the vendor
	 */
	public String getVendor() {
		return vendor;
	}
	/**
	 * @param vendor the vendor to set
	 */
	void setVendor(String vendor) {
		this.vendor = vendor;
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
	
}
