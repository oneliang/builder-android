package com.oneliang.tools.builder.android.handler;

import com.oneliang.util.file.FileUtil;

public class CleanHandler extends AbstractAndroidHandler {

	public boolean handle() {
		if(this.androidConfiguration.isNeedToClean()){
			FileUtil.deleteAllFile(this.androidConfiguration.getBuildOutput());
		}
		return true;
	}
}
