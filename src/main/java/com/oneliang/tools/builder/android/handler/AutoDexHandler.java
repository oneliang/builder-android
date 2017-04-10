package com.oneliang.tools.builder.android.handler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.oneliang.Constant;
import com.oneliang.tools.autodex.AutoDexUtil;
import com.oneliang.tools.builder.android.base.AndroidProject;
import com.oneliang.tools.builder.base.BuildException;
import com.oneliang.util.file.FileUtil;

public class AutoDexHandler extends AbstractAndroidHandler {

    public boolean handle() {
        this.autoDex();
        return true;
    }

    protected void autoDex() {
        try {
            String autoDexOutput = this.androidConfiguration.getMainAndroidProject().getAutoDexOutput();
            String androidManifestFullFilename = this.androidConfiguration.getPublicAndroidProject().getAndroidManifestOutput();
            boolean debug = this.androidConfiguration.isApkDebug();
            List<String> combinedClassList = this.getAllAndroidProjectClassesList();
            AutoDexUtil.Option option = new AutoDexUtil.Option(combinedClassList, androidManifestFullFilename, autoDexOutput, debug);
            option.minMainDex = true;
            option.attachBaseContext = true;
            option.mainDexOtherClassList = this.androidConfiguration.getAutoDexMainDexOtherClassList();
            AutoDexUtil.autoDex(option);

            final String prepareOutput = this.androidConfiguration.getMainAndroidProject().getPrepareOutput();
            FileUtil.MatchOption matchOption = new FileUtil.MatchOption(autoDexOutput);
            matchOption.fileSuffix = Constant.Symbol.DOT + Constant.File.DEX;
            matchOption.deep = false;
            matchOption.processor = new FileUtil.MatchOption.Processor() {
                public String onMatch(File file) {
                    FileUtil.copyFile(file.getAbsolutePath(), prepareOutput, FileUtil.FileCopyType.FILE_TO_PATH);
                    return null;
                }
            };
            FileUtil.findMatchFile(matchOption);
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    private List<String> getAllAndroidProjectClassesList() {
        List<String> classesList = new ArrayList<String>();
        List<AndroidProject> androidProjectList = this.androidConfiguration.getAndroidProjectList();
        for (AndroidProject androidProject : androidProjectList) {
            classesList.add(androidProject.getClassesOutput());
            classesList.addAll(androidProject.getDependJarSet());
        }
        classesList.add(this.androidConfiguration.getPublicAndroidProject().getClassesOutput());
        return filterDuplicateFile(classesList);
    }
}
