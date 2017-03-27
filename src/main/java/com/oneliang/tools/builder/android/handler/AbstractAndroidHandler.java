package com.oneliang.tools.builder.android.handler;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oneliang.Constant;
import com.oneliang.tools.builder.android.base.Android;
import com.oneliang.tools.builder.android.base.AndroidConfiguration;
import com.oneliang.tools.builder.android.base.AndroidProject;
import com.oneliang.tools.builder.android.base.PublicAndroidProject;
import com.oneliang.tools.builder.android.template.TemplateConstant;
import com.oneliang.tools.builder.base.Configuration;
import com.oneliang.tools.builder.java.handler.AbstractJavaHandler;
import com.oneliang.util.common.Generator;
import com.oneliang.util.common.StringUtil;
import com.oneliang.util.file.FileUtil;
import com.oneliang.util.logging.Logger;
import com.oneliang.util.logging.LoggerManager;

public abstract class AbstractAndroidHandler extends AbstractJavaHandler {

    protected static final Logger logger = LoggerManager.getLogger(AbstractAndroidHandler.class);

    public static final String CACHE_AIDL_FILE = "aidlFile";
    public static final String CACHE_LINEAR_ALLOC = "linearAlloc";
    public static final String CACHE_RESOURCE_FILE = "resourceFile";
    public static final String CACHE_ASSETS_FILE = "assetsFile";
    public static final String CACHE_RESOURCE_ITEM = "resourceItem";
    public static final String CACHE_SO_FILE = "soFile";

    protected AndroidConfiguration androidConfiguration = null;
    protected Android android = null;

    public void setConfiguration(Configuration configuration) {
        super.setConfiguration(configuration);
        if (configuration != null && (configuration instanceof AndroidConfiguration)) {
            this.androidConfiguration = (AndroidConfiguration) configuration;
            this.java = this.androidConfiguration.getJava();
            this.android = this.androidConfiguration.getAndroid();
        }
    }

    /**
     * is all compile file has cache
     * 
     * @param androidProjectList
     * @return boolean
     */
    protected final boolean isAllCompileFileHasCache(List<AndroidProject> androidProjectList) {
        boolean allCompileFileHasCache = true;
        for (AndroidProject androidProject : androidProjectList) {
            if (!androidProject.isAllCompileFileHasCache()) {
                allCompileFileHasCache = false;
                break;
            }
        }
        return allCompileFileHasCache;
    }

    /**
     * generate empty android manifest
     * 
     * @param outputFullFilename
     */
    protected final void generateEmptyAndroidManifest(String outputFullFilename) {
        InputStream templateInputStream = null;
        try {
            templateInputStream = TemplateConstant.getTemplateInputStream(TemplateConstant.Template.ANDROID_MANIFEST);
            Map<String, String> valueMap = new HashMap<String, String>();
            valueMap.put("#PACKAGE#", this.androidConfiguration.getMainAndroidProject().getPackageName());
            FileUtil.generateSimpleFile(templateInputStream, outputFullFilename, valueMap);
            templateInputStream.close();
        } catch (Exception e) {
            logger.error("generate empty android manifest failure.", e);
        } finally {
            if (templateInputStream != null) {
                try {
                    templateInputStream.close();
                } catch (Exception e) {
                    logger.error("generate empty android manifest stream close failure.", e);
                }
            }
        }
    }

    /**
     * get android project jar list include libs jar without proguard
     * 
     * @param androidProject
     * @return List<String>
     */
    protected final List<String> getAndroidProjectJarListWithoutProguard(AndroidProject androidProject) {
        return getAndroidProjectJarList(androidProject, true);
    }

    /**
     * get android project jar list include libs jar with proguard
     * 
     * @param androidProject
     * @return List<String>
     */
    protected final List<String> getAndroidProjectJarListWithProguard(AndroidProject androidProject) {
        return getAndroidProjectJarList(androidProject, false);
    }

    /**
     * get android project jar list include libs jar
     * 
     * @param androidProject
     * @return List<String>
     */
    private final List<String> getAndroidProjectJarList(AndroidProject androidProject, boolean original) {
        List<String> classesJarListAndLibraryList = new ArrayList<String>();

        String projectClassesJar = (original ? androidProject.getOptimizedOriginalOutput() : androidProject.getOptimizedProguardOutput()) + "/" + androidProject.getName() + Constant.Symbol.DOT + Constant.File.JAR;
        if (FileUtil.isExist(projectClassesJar)) {
            classesJarListAndLibraryList.add(projectClassesJar);
        }

        List<String> jarList = androidProject.getDependJarList();
        if (jarList != null && !jarList.isEmpty()) {
            for (String jar : jarList) {
                // maybe same jar filename
                String jarFilename = Generator.MD5File(jar) + Constant.Symbol.DOT + Constant.File.JAR;
                String classesJar = (original ? androidProject.getOptimizedOriginalOutput() : androidProject.getOptimizedProguardOutput()) + "/" + jarFilename;
                if (!classesJarListAndLibraryList.contains(classesJar)) {
                    classesJarListAndLibraryList.add(classesJar);
                }
            }
        }

        if (androidProject.getName().equals(this.androidConfiguration.getProjectMain())) {
            String publicJar = (original ? this.androidConfiguration.getPublicAndroidProject().getOptimizedOriginalOutput() : this.androidConfiguration.getPublicAndroidProject().getOptimizedProguardOutput()) + "/" + PublicAndroidProject.PUBLIC + Constant.Symbol.DOT + Constant.File.JAR;
            if (FileUtil.isExist(publicJar)) {
                classesJarListAndLibraryList.add(publicJar);
            }
            String publicRJar = (original ? this.androidConfiguration.getPublicRAndroidProject().getOptimizedOriginalOutput() : this.androidConfiguration.getPublicRAndroidProject().getOptimizedProguardOutput()) + "/" + PublicAndroidProject.PUBLIC_R + Constant.Symbol.DOT + Constant.File.JAR;
            if (FileUtil.isExist(publicRJar)) {
                classesJarListAndLibraryList.add(publicRJar);
            }
        }
        return classesJarListAndLibraryList;
    }

    /**
     * filter duplicate file
     * 
     * @param fileList
     * @return List<String>
     */
    protected List<String> filterDuplicateFile(List<String> fileList) {
        if (fileList == null || fileList.isEmpty()) {
            return null;
        }
        List<String> list = new ArrayList<String>();
        for (String file : fileList) {
            String fileAbsolutePath = new File(file).getAbsolutePath();
            if (!list.contains(fileAbsolutePath)) {
                list.add(fileAbsolutePath);
            }
        }
        return list;
    }

    /**
     * optimize name
     * 
     * @param name,for
     *            project name and other string
     * @return String
     */
    protected String optimizeName(String name) {
        return StringUtil.nullToBlank(name).replace(Constant.Symbol.SLASH_LEFT, Constant.Symbol.MINUS).replace(Constant.Symbol.SLASH_RIGHT, Constant.Symbol.MINUS);
    }
}
