package com.oneliang.tools.builder.android.handler;

import java.io.File;
import java.util.Set;

import com.oneliang.Constants;
import com.oneliang.tools.builder.base.BuilderUtil;
import com.oneliang.util.common.Generator;
import com.oneliang.util.common.StringUtil;
import com.oneliang.util.file.FileUtil;

public class AfterCompileAndroidProjectHandler extends AndroidProjectHandler {

    public boolean handle() {
        if (this.androidProject.isAllCompileFileHasNotChanged()) {
            return true;
        }
        String jarOutput = this.androidConfiguration.isApkDebug() ? androidProject.getOptimizedProguardOutput() : androidProject.getOptimizedOriginalOutput();
        String androidProjectJar = jarOutput + "/" + optimizeName(this.androidProject.getName()) + Constants.Symbol.DOT + Constants.File.JAR;
        FileUtil.createDirectory(jarOutput);
        // is not android aapt generate r file,delete R$*.class
        if (!this.androidConfiguration.isAaptGenerateRFile()) {
//            String packageDirectory = androidProject.getClassesOutput() + Constants.Symbol.SLASH_LEFT + androidProject.getPackageName().replace(Constants.Symbol.DOT, Constants.Symbol.SLASH_LEFT);
//            File[] packageDirectoryFileArray = new File(packageDirectory).listFiles();
//            if (packageDirectoryFileArray != null) {
//                for (File file : packageDirectoryFileArray) {
//                    if (file.isFile()) {
//                        String filename = file.getName();
//                        if (StringUtil.isMatchPattern(filename, "R\\$*.class") || StringUtil.isMatchPattern(filename, "R.class")) {
//                            file.delete();
//                        }
//                    }
//                }
//            }
        }
        BuilderUtil.jar(androidProjectJar, androidProject.getClassesOutput());
        Set<String> jarSet = androidProject.getDependJarSet();
        for (String jar : jarSet) {
            String jarFilename = Generator.MD5File(jar);
            String jarOutputFullFilename = jarOutput + Constants.Symbol.SLASH_LEFT + jarFilename + Constants.Symbol.DOT + Constants.File.JAR;
            FileUtil.copyFile(jar, jarOutputFullFilename, FileUtil.FileCopyType.FILE_TO_FILE);
        }
        return true;
    }
}
