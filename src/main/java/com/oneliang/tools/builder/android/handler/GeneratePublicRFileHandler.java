package com.oneliang.tools.builder.android.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.oneliang.tools.builder.android.base.AndroidProject;
import com.oneliang.tools.builder.base.BuilderUtil;
import com.oneliang.util.file.FileUtil;

public class GeneratePublicRFileHandler extends AbstractAndroidHandler {

	private boolean generateSuccess=false;

	public boolean handle() {
		final String destinationDirectory=this.androidConfiguration.getPublicRAndroidProject().getGenOutput();
		FileUtil.createDirectory(destinationDirectory);
		final Map<String,String> packageNameAndroidManifestMap=this.androidConfiguration.getPackageNameAndroidManifestMap();
		final String resourceFileCacheFullFilename=this.androidConfiguration.getPublicRAndroidProject().getCacheOutput()+"/"+CACHE_RESOURCE_FILE;
		final List<String> resourceDirectoryList=this.androidConfiguration.findDirectoryOfAndroidProjectList(this.androidConfiguration.getAndroidProjectList(),AndroidProject.DirectoryType.RES);
		CacheOption cacheOption=new CacheOption(resourceFileCacheFullFilename, resourceDirectoryList);
		cacheOption.changedFileProcessor=new ChangedFileProcessor() {
			public boolean process(Iterable<ChangedFile> changedFileIterable) {
				boolean result=false;
				if(changedFileIterable!=null&&changedFileIterable.iterator().hasNext()){
					Iterator<Entry<String,String>> packageNameAndroidManifestIterator=packageNameAndroidManifestMap.entrySet().iterator();
					while(packageNameAndroidManifestIterator.hasNext()){
						Entry<String,String> entry=packageNameAndroidManifestIterator.next();
						String androidManifest=entry.getValue();
						List<String> resourceDirectoryList=new ArrayList<String>(androidConfiguration.findDirectoryOfAndroidProjectList(androidConfiguration.getAndroidProjectList(),AndroidProject.DirectoryType.RES));
						if(FileUtil.isExist(androidConfiguration.getPublicRAndroidProject().getResourceOutput())){
							resourceDirectoryList.add(androidConfiguration.getPublicRAndroidProject().getResourceOutput());
						}
						int aaptResult=BuilderUtil.executeAndroidAaptToGenerateR(android.getAaptExecutor(),androidManifest,resourceDirectoryList,destinationDirectory,Arrays.asList(androidConfiguration.getMainAndroidApiJar()),androidConfiguration.isApkDebug());
						logger.info("Aapt generate R result code:"+aaptResult);
						if(aaptResult==0){
							result=true;
						}
					}
				}else{
					androidConfiguration.setAllResourceFileHasCache(true);
					result=true;
				}
				if(result){
					generateSuccess=true;
				}
				return result;				
			}
		};
		this.dealWithCache(cacheOption);
		return generateSuccess;
	}
}
