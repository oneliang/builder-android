package com.oneliang.tools.builder.android.handler;

import com.oneliang.tools.builder.android.base.AndroidProject;
import com.oneliang.tools.builder.base.BuilderUtil;
import com.oneliang.util.file.FileUtil;

public class GenerateApkHandler extends AbstractAndroidHandler {

	public boolean handle() {
		FileUtil.createDirectory(this.androidConfiguration.getMainAndroidProject().getPrepareOutput());
		FileUtil.createDirectory(this.androidConfiguration.getMainAndroidProject().getPrepareAssetsOutput());
		FileUtil.createDirectory(this.androidConfiguration.getMainAndroidProject().getPrepareLibOutput());
		if(!this.androidConfiguration.isAllAssetsFileHasCache()||!isAllCompileFileHasCache(this.androidConfiguration.getAndroidProjectList())){
			String dexFullFilename=this.androidConfiguration.getMainAndroidProject().getDexOutput()+"/"+AndroidProject.CLASSES_DEX;
			String apkOutputFullFilename=this.androidConfiguration.getMainAndroidProject().getUnsignedApkFullFilename();
//			BuilderUtil.androidApkBuilder(apkOutputFullFilename, resourceFullFilename, dexFullFilename, libDirectoryList,this.configuration.isApkDebug());

			//copy main dex to publish prepare
			FileUtil.copyFile(dexFullFilename, this.androidConfiguration.getMainAndroidProject().getPrepareOutput(), FileUtil.FileCopyType.FILE_TO_PATH);
			BuilderUtil.generateApk(this.androidConfiguration.getMainAndroidProject().getPrepareOutput(), apkOutputFullFilename);
		}
		return true;
	}
}
