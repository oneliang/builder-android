package com.oneliang.tools.builder.android.handler;

import java.io.File;

import com.oneliang.tools.builder.base.BuilderUtil;

public class ZipAlignApkHandler extends AbstractAndroidHandler {

	public boolean handle() {
		if(!this.androidConfiguration.isAllAssetsFileHasCache()||!isAllCompileFileHasCache(this.androidConfiguration.getAndroidProjectList())){
			String inputApkFullFilename=this.androidConfiguration.getMainAndroidProject().getApkFullFilename();
			String outputApkFullFilename=this.androidConfiguration.getMainAndroidProject().getZipAlignApkFullFilename();
			new File(outputApkFullFilename).delete();
			BuilderUtil.executeZipAlign(this.android.getZipAlignExecutor(), 4, inputApkFullFilename, outputApkFullFilename);
		}
		return true;
	}
}
