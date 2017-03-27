package com.oneliang.tools.builder.android.base;

import java.io.File;

import com.oneliang.tools.builder.base.Project;


public class PatchAndroidProject {

	private String name=null;
	private String outputHome=null;
	private String genOutput=null;
	private String sourceOutput=null;
	private String resourceOutput=null;
	private String resourceModifyOutput=null;
	private String prepareOutput=null;
	private String differentOutput=null;
	private String androidManifestOutput=null;
	private String unsignedApkFullFilename=null;
	private String apkFullFilename=null;
	private String zipAlignApkFullFilename=null;
	private boolean debug=false;

	public PatchAndroidProject(String outputHome, boolean debug) {
		this.name="patch";
		this.debug=debug;
		File file=new File(outputHome);
		this.outputHome=file.getAbsolutePath()+"/"+this.name;
		this.genOutput=this.outputHome+"/"+AndroidProject.GEN;
		this.sourceOutput=this.outputHome+"/"+"src";
		this.resourceOutput=this.outputHome+"/"+AndroidProject.RES;
		this.resourceModifyOutput=this.outputHome+"/"+AndroidProject.RES+"Modify";
		this.prepareOutput=this.outputHome+"/prepare";
		this.differentOutput=this.outputHome+"/different";
		this.androidManifestOutput=this.outputHome+"/"+AndroidProject.ANDROID_MANIFEST;
		this.unsignedApkFullFilename=this.outputHome+"/"+this.name+AndroidProject.UNSIGNED_APK_SUFFIX;
		this.apkFullFilename=this.outputHome+"/"+this.name+(this.debug?AndroidProject.DEBUG_APK_SUFFIX:AndroidProject.RELEASE_APK_SUFFIX);
		this.zipAlignApkFullFilename=this.outputHome+"/"+this.name+(this.debug?AndroidProject.DEBUG_ZIP_ALIGN_APK_SUFFIX:AndroidProject.RELEASE_ZIP_ALIGN_APK_SUFFIX);
	}

	/**
	 * @return the genOutput
	 */
	public String getGenOutput() {
		return genOutput;
	}

	/**
	 * @return the androidManifestOutput
	 */
	public String getAndroidManifestOutput() {
		return androidManifestOutput;
	}

	/**
	 * @return the outputHome
	 */
	public String getOutputHome() {
		return outputHome;
	}

	/**
	 * @return the resourceOutput
	 */
	public String getResourceOutput() {
		return resourceOutput;
	}

	/**
	 * @return the prepareOutput
	 */
	public String getPrepareOutput() {
		return prepareOutput;
	}

	/**
	 * @return the differentOutput
	 */
	public String getDifferentOutput() {
		return differentOutput;
	}

	/**
	 * @return the unsignedApkFullFilename
	 */
	public String getUnsignedApkFullFilename() {
		return unsignedApkFullFilename;
	}

	/**
	 * @return the apkFullFilename
	 */
	public String getApkFullFilename() {
		return apkFullFilename;
	}

	/**
	 * @return the zipAlignApkFullFilename
	 */
	public String getZipAlignApkFullFilename() {
		return zipAlignApkFullFilename;
	}

	/**
	 * @return the resourceModifyOutput
	 */
	public String getResourceModifyOutput() {
		return resourceModifyOutput;
	}

	/**
	 * @return the sourceOutput
	 */
	public String getSourceOutput() {
		return sourceOutput;
	}
}
