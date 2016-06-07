package com.oneliang.tools.builder.android.handler;

import java.util.ArrayList;
import java.util.List;

import com.oneliang.tools.builder.android.base.AndroidProject;
import com.oneliang.tools.builder.base.BuilderUtil;
import com.oneliang.util.file.FileUtil;

public class MergeAndroidManifestHandler extends AbstractAndroidHandler {

	public boolean handle() {
		List<String> libraryAndroidManifestList=new ArrayList<String>();
		for(AndroidProject androidProject:this.androidConfiguration.getAndroidProjectList()){
//			if(!this.androidConfiguration.getProjectMain().equals(androidProject.getName())){
				libraryAndroidManifestList.addAll(androidProject.getAndroidManifestList());
//			}
		}
		String mainProjectAndroidManifest=libraryAndroidManifestList.remove(0);//this.androidConfiguration.getMainAndroidProject().getAndroidManifest();
//		logger.log("main android manifest:"+mainProjectAndroidManifest+",library android manifest size:"+libraryAndroidManifestList.size());
		String androidManifestOutput=this.androidConfiguration.getPublicAndroidProject().getAndroidManifestOutput();
		FileUtil.createFile(androidManifestOutput);
		BuilderUtil.mergeAndroidManifest(mainProjectAndroidManifest, libraryAndroidManifestList, androidManifestOutput,true);//this.androidConfiguration.isApkDebug());
		return true;
	}
}
