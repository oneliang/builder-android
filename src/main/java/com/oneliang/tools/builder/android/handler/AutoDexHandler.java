package com.oneliang.tools.builder.android.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.oneliang.Constant;
import com.oneliang.tools.autodex.AutoDexUtil;
import com.oneliang.tools.builder.android.base.AndroidProject;
import com.oneliang.tools.builder.base.BuildException;
import com.oneliang.tools.builder.base.ChangedFile;
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
            String cacheFullFilename = this.androidConfiguration.getMainAndroidProject().getCacheOutput()+Constant.Symbol.SLASH_LEFT+CACHE_DEX_FILE;
            CacheOption cacheOption = new CacheOption(cacheFullFilename, Arrays.asList(autoDexOutput));
            cacheOption.fileSuffix = Constant.Symbol.DOT + Constant.File.DEX;
            cacheOption.changedFileProcessor = new CacheOption.ChangedFileProcessor() {
                public boolean process(Iterable<ChangedFile> changedFileIterable) {
                    boolean saveCache = false;
                    if (changedFileIterable != null && changedFileIterable.iterator().hasNext()) {
                        Iterator<ChangedFile> changedFileIterator = changedFileIterable.iterator();
                        while (changedFileIterator.hasNext()) {
                            ChangedFile changedFile = changedFileIterator.next();
                            if (changedFile.status.equals(ChangedFile.Status.DELETED)) {
                                continue;
                            }
                            FileUtil.copyFile(changedFile.fullFilename, prepareOutput, FileUtil.FileCopyType.FILE_TO_PATH);
                        }
                        saveCache = true;
                        androidConfiguration.getMainAndroidProject().setAllCompileFileHasNotChanged(false);
                    }
                    return saveCache;
                }
            };
            this.dealWithCache(cacheOption);
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    private List<String> getAllAndroidProjectClassesList() {
        List<String> classesList = new ArrayList<String>();
        List<AndroidProject> androidProjectList = this.androidConfiguration.getAndroidProjectList();
        for (AndroidProject androidProject : androidProjectList) {
            if(androidProject.isProvided()){
                continue;
            }
            classesList.add(androidProject.getClassesOutput());
            classesList.addAll(androidProject.getDependJarSet());
        }
        classesList.add(this.androidConfiguration.getPublicAndroidProject().getClassesOutput());
        return filterDuplicateFile(classesList);
    }
}
