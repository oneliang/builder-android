package com.oneliang.tools.builder.android.handler;

import java.util.ArrayList;
import java.util.List;

import com.oneliang.tools.builder.android.base.AndroidProject;
import com.oneliang.util.file.FileUtil;

public class MergeAllAndroidProjectJarHandler extends AbstractAndroidHandler {

	public boolean handle() {
		List<AndroidProject> androidProjectList=this.androidConfiguration.getAndroidProjectList();
		List<String> allAndroidProjectOriginalJarList=new ArrayList<String>();
		List<String> allAndroidProjectJarList=new ArrayList<String>();
		for(AndroidProject androidProject:androidProjectList){
			if(!this.androidConfiguration.isApkDebug()){
				allAndroidProjectOriginalJarList.addAll(this.getAndroidProjectJarListWithoutProguard(androidProject));
			}
			allAndroidProjectJarList.addAll(this.getAndroidProjectJarListWithProguard(androidProject));
		}
		if(!this.androidConfiguration.isApkDebug()){
			final String allOriginalClassesJar=this.androidConfiguration.getMainAndroidProject().getAutoDexAllOriginalClassesJar();
			FileUtil.mergeZip(allOriginalClassesJar, allAndroidProjectOriginalJarList);
		}
		final String allClassesJar=this.androidConfiguration.getMainAndroidProject().getAutoDexAllClassesJar();
		FileUtil.mergeZip(allClassesJar, allAndroidProjectJarList);
		return true;
	}
}
