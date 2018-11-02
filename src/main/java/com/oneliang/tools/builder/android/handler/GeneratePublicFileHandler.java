package com.oneliang.tools.builder.android.handler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.oneliang.Constants;
import com.oneliang.tools.builder.android.base.PublicAndroidProject;
import com.oneliang.tools.builder.android.template.ConstantsTemplate;
import com.oneliang.util.common.StringUtil;
import com.oneliang.util.file.FileUtil;

public class GeneratePublicFileHandler extends AbstractAndroidHandler {
    protected static final String CONSTANT_OTHER_CONTENT = "#OTHER_CONTENT#";

    public boolean handle() {
        this.generatePublicBuildConfigFile(null, null);
        return true;
    }

    /**
     * generate public build config file
     */
    protected void generatePublicBuildConfigFile(Map<String, byte[]> packageTemplateByteArrayMap, Map<String, Map<String, String>> packageBuildConfigValueMap) {
        String destinationDirectory = this.androidConfiguration.getPublicAndroidProject().getGenOutput();
        FileUtil.createDirectory(destinationDirectory);
        Map<String, String> packageNameAndroidManifestMap = this.androidConfiguration.getPackageNameAndroidManifestMap();
        Iterator<Entry<String, String>> packageNameAndroidManifestIterator = packageNameAndroidManifestMap.entrySet().iterator();
        while (packageNameAndroidManifestIterator.hasNext()) {
            Entry<String, String> entry = packageNameAndroidManifestIterator.next();
            String packageName = entry.getKey();

            InputStream templateInputStream = null;
            if (packageTemplateByteArrayMap != null && packageTemplateByteArrayMap.containsKey(packageName)) {
                templateInputStream = new ByteArrayInputStream(packageTemplateByteArrayMap.get(packageName));
            } else {
                templateInputStream = ConstantsTemplate.getTemplateInputStream(ConstantsTemplate.Template.BUILD_CONFIG);
            }
            String outputFullFilename = destinationDirectory + Constants.Symbol.SLASH_LEFT + packageName.replace(Constants.Symbol.DOT, Constants.Symbol.SLASH_LEFT) + Constants.Symbol.SLASH_LEFT + PublicAndroidProject.BUILD_CONFIG;
            Map<String, String> valueMap = null;
            if (packageBuildConfigValueMap != null && packageBuildConfigValueMap.containsKey(packageName)) {
                valueMap = packageBuildConfigValueMap.get(packageName);
            } else {
                valueMap = new HashMap<String, String>();
            }
            valueMap.put("#PACKAGE#", packageName);
            valueMap.put("#DEBUG#", String.valueOf(this.androidConfiguration.isApkDebug()));
            if (!valueMap.containsKey(CONSTANT_OTHER_CONTENT)) {
                valueMap.put(CONSTANT_OTHER_CONTENT, StringUtil.BLANK);
            }
            FileUtil.generateSimpleFile(templateInputStream, outputFullFilename, valueMap);
        }
    }
}
