package com.oneliang.tools.builder.android.base;

import java.util.Properties;

import com.oneliang.Constants;
import com.oneliang.util.common.StringUtil;

public class BuildTool {

	static final String SOURCE_PROPERTIES="source.properties";

	private static final String PKG_REVISION="Pkg.Revision";

	private String pkgRevision=null;
	private String directory=null;
	private int pkgRevisionInt=0;

	public BuildTool(Properties properties){
		this.pkgRevision=properties.getProperty(PKG_REVISION, StringUtil.BLANK).trim();
		String[] array=pkgRevision.split(Constants.Symbol.SLASH_RIGHT+Constants.Symbol.DOT);
		final int maxWeight=10000;
		final int weightLevel=100;
		for(int i=0;i<array.length;i++){
			this.pkgRevisionInt+=Integer.parseInt(array[i])*maxWeight/Math.round(Math.pow(weightLevel, i));
		}
	}

	/**
	 * @return the pkgRevision
	 */
	public String getPkgRevision() {
		return pkgRevision;
	}

	/**
	 * @param pkgRevision the pkgRevision to set
	 */
	void setPkgRevision(String pkgRevision) {
		this.pkgRevision = pkgRevision;
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
	 * @return the pkgRevisionInt
	 */
	public int getPkgRevisionInt() {
		return pkgRevisionInt;
	}
}
