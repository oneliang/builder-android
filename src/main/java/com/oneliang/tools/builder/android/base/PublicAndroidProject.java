package com.oneliang.tools.builder.android.base;

import java.io.File;
import java.util.List;

import com.oneliang.tools.builder.base.Project;


public class PublicAndroidProject {

	public static final String PUBLIC="public";
	public static final String PUBLIC_R="publicR";

	public static final String R_TXT="R.txt";
	public static final String BUILD_CONFIG="BuildConfig.java";
	//public file output directory
	private String name=null;
	private String outputHome=null;
	private String genOutput=null;
	private String genOriginalOutput=null;
	private String classesOutput=null;
	private String cacheOutput=null;
	private String resourceOutput=null;
	private String resourceOriginalOutput=null;
	private String optimizedOriginalOutput=null;
	private String optimizedProguardOutput=null;
	private String androidManifestOutput=null;
	//use in building
	private List<String> thisTimeClassFileList=null;

	public PublicAndroidProject(String outputHome,String name) {
		this.name=name;
		File file=new File(outputHome);
		this.outputHome=file.getAbsolutePath()+"/"+this.name+"/"+Project.BUILD;
		this.genOutput=this.outputHome+"/"+AndroidProject.GEN;
		this.genOriginalOutput=this.outputHome+"/"+AndroidProject.GEN+"Original";
		this.classesOutput=this.outputHome+"/"+Project.CLASSES;
		this.cacheOutput=this.outputHome+"/"+Project.CACHE;
		this.resourceOutput=this.outputHome+"/"+AndroidProject.RES;
		this.resourceOriginalOutput=this.outputHome+"/"+AndroidProject.RES+"Original";
		this.optimizedOriginalOutput=this.outputHome+"/optimized/original";
		this.optimizedProguardOutput=this.outputHome+"/optimized/proguard";
		this.androidManifestOutput=this.outputHome+"/"+AndroidProject.ANDROID_MANIFEST;
	}

	/**
	 * @return the genOutput
	 */
	public String getGenOutput() {
		return genOutput;
	}

	/**
	 * @return the classesOutput
	 */
	public String getClassesOutput() {
		return classesOutput;
	}

	/**
	 * @return the cacheOutput
	 */
	public String getCacheOutput() {
		return cacheOutput;
	}

	/**
	 * @return the resourceOutput
	 */
	public String getResourceOutput() {
		return resourceOutput;
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
	 * @return the androidManifestOutput
	 */
	public String getAndroidManifestOutput() {
		return androidManifestOutput;
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
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the genOriginalOutput
	 */
	public String getGenOriginalOutput() {
		return genOriginalOutput;
	}

	/**
	 * @return the resourceOriginalOutput
	 */
	public String getResourceOriginalOutput() {
		return resourceOriginalOutput;
	}

    /**
     * @return the outputHome
     */
    public String getOutputHome() {
        return outputHome;
    }
}
