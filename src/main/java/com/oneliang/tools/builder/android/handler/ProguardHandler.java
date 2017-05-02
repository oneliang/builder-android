package com.oneliang.tools.builder.android.handler;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.oneliang.Constant;
import com.oneliang.tools.builder.android.base.AndroidProject;
import com.oneliang.tools.builder.android.base.PublicAndroidProject;
import com.oneliang.tools.builder.base.BuilderUtil;
import com.oneliang.tools.builder.base.BuilderUtil.ProguardJarPair;
import com.oneliang.util.common.Generator;
import com.oneliang.util.common.StringUtil;
import com.oneliang.util.file.FileUtil;

public class ProguardHandler extends AbstractAndroidHandler {

    public boolean handle() {
        if (!androidConfiguration.isApkDebug()) {
            List<String> classpathList = new ArrayList<String>();

            String googleApiTarget = this.androidConfiguration.findMaxGoogleApiTargetInAllAndroidProject();
            String androidApiTarget = this.androidConfiguration.findMaxAndroidApiTargetInAllAndroidProject();
            logger.info("Proguard android api target:" + androidApiTarget + ",google api target:" + googleApiTarget);
            if (!StringUtil.isBlank(googleApiTarget)) {// has google api
                if (!StringUtil.isBlank(androidApiTarget)) {// has android api
                    classpathList.add(this.android.findAndroidApiJar(androidApiTarget));
                    classpathList.addAll(this.android.findGoogleApiJarList(googleApiTarget));
                } else {
                    classpathList.addAll(this.android.findApiJarList(googleApiTarget));
                }
            } else {
                classpathList.add(this.android.findAndroidApiJar(androidApiTarget));
            }
            List<String> proguardClasspathList = this.androidConfiguration.getProguardClasspathList();
            if (proguardClasspathList != null) {
                classpathList.addAll(proguardClasspathList);
            }

            FileUtil.createDirectory(this.androidConfiguration.getMainAndroidProject().getOptimizedProguardOutput());
            AndroidProject mainAndroidProject = this.androidConfiguration.getMainAndroidProject();
            List<ProguardJarPair> proguardJarPairList = new ArrayList<ProguardJarPair>();
            // public
            String inputPublicJar = this.androidConfiguration.getPublicAndroidProject().getOptimizedOriginalOutput() + "/" + PublicAndroidProject.PUBLIC + Constant.Symbol.DOT + Constant.File.JAR;
            String outputPublicJar = this.androidConfiguration.getPublicAndroidProject().getOptimizedProguardOutput() + "/" + PublicAndroidProject.PUBLIC + Constant.Symbol.DOT + Constant.File.JAR;
            proguardJarPairList.add(new ProguardJarPair(inputPublicJar, outputPublicJar));
            // public r
            String inputPublicRJar = this.androidConfiguration.getPublicRAndroidProject().getOptimizedOriginalOutput() + "/" + PublicAndroidProject.PUBLIC_R + Constant.Symbol.DOT + Constant.File.JAR;
            if (FileUtil.isExist(inputPublicRJar)) {
                String outputPublicRJar = this.androidConfiguration.getPublicRAndroidProject().getOptimizedProguardOutput() + "/" + PublicAndroidProject.PUBLIC_R + Constant.Symbol.DOT + Constant.File.JAR;
                proguardJarPairList.add(new ProguardJarPair(inputPublicRJar, outputPublicRJar));
            }
            Map<String, String> jarMap = new HashMap<String, String>();
            for (AndroidProject androidProject : this.androidConfiguration.getAndroidProjectList()) {
                Set<String> jarSet = androidProject.getDependJarSet();
                String optimizeProjectName = optimizeName(androidProject.getName());
                String inputAndroidProjectJar = androidProject.getOptimizedOriginalOutput() + "/" + optimizeProjectName + Constant.Symbol.DOT + Constant.File.JAR;
                if (FileUtil.isExist(inputAndroidProjectJar)) {
                    String outputAndroidProjectJar = androidProject.getOptimizedProguardOutput() + "/" + optimizeProjectName + Constant.Symbol.DOT + Constant.File.JAR;
                    proguardJarPairList.add(new ProguardJarPair(inputAndroidProjectJar, outputAndroidProjectJar));
                }
                for (String jar : jarSet) {
                    File jarFile = new File(jar);
                    String jarFilename = Generator.MD5File(jarFile.getAbsolutePath()) + Constant.Symbol.DOT + Constant.File.JAR;
                    if (!jarMap.containsKey(jarFilename)) {
                        String inputJar = androidProject.getOptimizedOriginalOutput() + "/" + jarFilename;
                        String outputJar = androidProject.getOptimizedProguardOutput() + "/" + jarFilename;
                        proguardJarPairList.add(new ProguardJarPair(inputJar, outputJar));
                        jarMap.put(jarFilename, jarFilename);
                    }
                }
                List<String> onlyCompileClasspathList = androidProject.getOnlyCompileClasspathList();
                if (onlyCompileClasspathList != null) {
                    classpathList.addAll(onlyCompileClasspathList);
                }
            }
            List<String> proguardConfigList = null;
            if (this.androidConfiguration.isApkPreRelease()) {
                proguardConfigList = Arrays.asList(this.android.getProguardAndroidTxt(), mainAndroidProject.getProguardCfg());
            } else {
                proguardConfigList = Arrays.asList(this.android.getProguardAndroidOptimizeTxt(), mainAndroidProject.getProguardCfg());
            }
            classpathList = filterDuplicateFile(classpathList);
            for (String classpath : classpathList) {
                logger.info(classpath);
            }
            BuilderUtil.proguard(proguardConfigList, proguardJarPairList, classpathList);
            // unzip all zip for auto dex
            // if(this.configuration.isAutoDex()){
            // String
            // allClassesOutput=this.build.getClasses()+"/"+Build.ALL_CLASSES;
            // for(String originalJarFile:androidProjectClassesList){
            // String jarFile=this.build.getOptimizedProguard()+"/"+(new
            // File(originalJarFile).getName());
            // FileUtil.unzip(jarFile, allClassesOutput, null);
            // }
            // }
        }
        return true;
    }
}
