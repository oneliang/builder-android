package com.oneliang.tools.builder.android.handler;

import java.io.File;
import java.util.List;

import com.oneliang.Constant;
import com.oneliang.tools.builder.base.BuilderUtil;
import com.oneliang.util.common.StringUtil;
import com.oneliang.util.file.FileUtil;

public class AfterCompileAndroidProjectHandler extends AndroidProjectHandler {

	public boolean handle() {
		String jarOutput=this.androidConfiguration.isApkDebug()?androidProject.getOptimizedProguardOutput():androidProject.getOptimizedOriginalOutput();
		String androidProjectJar=jarOutput+"/"+androidProject.getName()+Constant.Symbol.DOT+Constant.File.JAR;
		FileUtil.createDirectory(jarOutput);
		//is not android aapt generate r file,delete R$*.class
		if(!this.androidConfiguration.isAaptGenerateRFile()){
			String packageDirectory=androidProject.getClassesOutput()+Constant.Symbol.SLASH_LEFT+androidProject.getPackageName().replace(Constant.Symbol.DOT, Constant.Symbol.SLASH_LEFT);
			File[] packageDirectoryFileArray=new File(packageDirectory).listFiles();
			if(packageDirectoryFileArray!=null){
				for(File file:packageDirectoryFileArray){
					if(file.isFile()){
						String filename=file.getName();
						if(StringUtil.isMatchPattern(filename, "R\\$*.class")||StringUtil.isMatchPattern(filename, "R.class")){
							file.delete();
						}
					}
				}
			}
		}
		BuilderUtil.jar(androidProjectJar, androidProject.getClassesOutput());
		List<String> jarList=androidProject.getDependJarList();
		for(String jar:jarList){
			FileUtil.copyFile(jar, jarOutput, FileUtil.FileCopyType.FILE_TO_PATH);
		}
		return true;
	}
}
