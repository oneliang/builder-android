package com.oneliang.tools.builder.android.handler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.oneliang.Constant;
import com.oneliang.tools.builder.android.aapt.AaptUtil;
import com.oneliang.tools.builder.android.aapt.RDotTxtEntry;
import com.oneliang.tools.builder.android.aapt.RDotTxtEntry.RType;
import com.oneliang.tools.builder.android.base.AndroidProject;
import com.oneliang.tools.builder.android.handler.InitializeAndroidProjectForGradleHandler.AarProject;
import com.oneliang.tools.builder.base.ChangedFile;
import com.oneliang.util.common.StringUtil;
import com.oneliang.util.file.FileUtil;

public class GenerateAarRDotJavaHandler extends AbstractAndroidHandler {

    public boolean handle() {
        @SuppressWarnings("unchecked")
        List<AarProject> aarProjectList = (List<AarProject>) this.androidConfiguration.getTemporaryData(InitializeAndroidProjectForGradleHandler.TEMPORARY_DATA_AAR_PROJECT_LIST);
        if (aarProjectList == null || aarProjectList.isEmpty()) {
            return true;
        }
        String rTxtFullFilename = this.androidConfiguration.getApkPatchInputRTxt();
        Map<RType, Set<RDotTxtEntry>> rTypeResourceMap = null;
        if (StringUtil.isNotBlank(rTxtFullFilename) && FileUtil.isExist(rTxtFullFilename)) {
            rTypeResourceMap = AaptUtil.readRTxt(rTxtFullFilename);
        } else {
            rTypeResourceMap = new HashMap<RType, Set<RDotTxtEntry>>();
        }
        final Map<RType, Set<RDotTxtEntry>> finalRTypeResourceMap = rTypeResourceMap;
        for (AarProject aarProject : aarProjectList) {
            final String aarResOutput = aarProject.unzipOutput + Constant.Symbol.SLASH_LEFT + AarProject.AAR_RES;
            final String aarAndroidManifest = aarProject.unzipOutput + Constant.Symbol.SLASH_LEFT + AarProject.AAR_ANDROID_MANIFEST_XML;
            if (FileUtil.hasFile(aarResOutput)) {
                String aarResCacheFullFilename = aarProject.unzipOutput + Constant.Symbol.SLASH_LEFT + CACHE_RESOURCE_FILE;
                CacheOption cacheOption = new CacheOption(aarResCacheFullFilename, Arrays.asList(aarResOutput));
                cacheOption.changedFileProcessor = new CacheOption.ChangedFileProcessor() {
                    public boolean process(Iterable<ChangedFile> changedFileIterable) {
                        if (changedFileIterable != null && changedFileIterable.iterator().hasNext()) {
                            Map<RType, Set<RDotTxtEntry>> allRTypeResourceMap = AaptUtil.collectResource(Arrays.asList(aarResOutput), finalRTypeResourceMap).getRTypeResourceMap();
                            String packageName = AndroidProject.parsePackageName(aarAndroidManifest);
                            String outputDirectory = androidConfiguration.getPublicAndroidProject().getGenOutput();
                            AaptUtil.writeRJava(outputDirectory, packageName, allRTypeResourceMap, !androidConfiguration.isApkDebug());
                            return true;
                        } else {
                            return false;
                        }
                    }
                };
                this.dealWithCache(cacheOption);
            }
        }
        return true;
    }
}
