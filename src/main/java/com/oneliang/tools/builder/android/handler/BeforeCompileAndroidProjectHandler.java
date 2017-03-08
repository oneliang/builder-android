package com.oneliang.tools.builder.android.handler;

import com.oneliang.util.file.FileUtil;

public class BeforeCompileAndroidProjectHandler extends AndroidProjectHandler {

	public boolean handle() {
		FileUtil.createDirectory(androidProject.getGenOutput());
		FileUtil.createDirectory(androidProject.getClassesOutput());
		FileUtil.createDirectory(androidProject.getCacheOutput());
		FileUtil.createDirectory(androidProject.getDexOutput());
		FileUtil.createDirectory(androidProject.getOptimizedOutput());
		return true;
	}
}
