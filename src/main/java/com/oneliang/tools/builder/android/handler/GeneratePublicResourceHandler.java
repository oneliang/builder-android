package com.oneliang.tools.builder.android.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.oneliang.Constant;
import com.oneliang.tools.builder.android.aapt.AaptResourceCollector;
import com.oneliang.tools.builder.android.aapt.AaptUtil;
import com.oneliang.tools.builder.android.aapt.RDotTxtEntry;
import com.oneliang.tools.builder.android.aapt.RDotTxtEntry.RType;
import com.oneliang.tools.builder.android.base.AndroidProject;
import com.oneliang.tools.builder.android.base.AndroidProject.DirectoryType;
import com.oneliang.tools.builder.base.Project;
import com.oneliang.util.common.StringUtil;
import com.oneliang.util.file.FileUtil;

public class GeneratePublicResourceHandler extends AbstractAndroidHandler{

	public boolean handle() {
		List<AndroidProject> androidProjectList=this.androidConfiguration.getAndroidProjectList();
		List<String> originalResourceDirectoryList=this.androidConfiguration.findDirectoryOfAndroidProjectList(androidProjectList, DirectoryType.RES);
		originalResourceDirectoryList = filterDuplicateFile(originalResourceDirectoryList);
		//1.read original r.txt
		//2.read the resource item properties,include resource entry cache and resource file cache
		//3.find the modify file(drawable,layout) and change to new file,keep all the modify resource key
		//4.find the modify string,color,dimen and copy to a new string color dimen.
		//5.find the increase file(drawable,layout) and copy that
		//6.iterate the modify resource key and modify all the reference resource file
		//7.generate public resource
		//1.read original r.txt
		String rTxtFullFilename=this.androidConfiguration.getApkPatchInputRTxt();
		Map<RType, Set<RDotTxtEntry>> rTypeResourceMap=null;
		if(StringUtil.isNotBlank(rTxtFullFilename)&&FileUtil.isExist(rTxtFullFilename)){
			rTypeResourceMap=AaptUtil.readRTxt(rTxtFullFilename);
		}else{
			rTypeResourceMap=new HashMap<RType, Set<RDotTxtEntry>>();
		}

		//2.read the resource item properties,include resource entry cache and resource file cache
		//3.find the modify file(drawable,layout) and change to new file,keep all the modify resource key
		//4.find the modify string,color,dimen and copy to a new string color dimen.
		//5.find the increase file(drawable,layout) and copy that
		AaptResourceCollector aaptResourceCollector=AaptUtil.collectResource(originalResourceDirectoryList, rTypeResourceMap);
		
//		Map<RType,Map<String,Set<ResourceEntry>>> modifiedResourceEntryMap=new HashMap<RType,Map<String,Set<ResourceEntry>>>();
//		this.generateIncreaseAndModifiedResource(originalResourceDirectoryList, rTypeResourceDirectoryMap, baseResourceItemMappingProperties, resourceItemMappingProperties, resourceItemMappingFullFilename, !rTypeResourceMap.isEmpty());
		
		//7.generate public resource
		//new resource directory list contain resource modify output
		//after find modify file,add this res folder to main project
		//before add res folder to main project,must keep the original res and r.txt
		String originalOutputIdsXmlFullFilename=this.androidConfiguration.getPublicRAndroidProject().getResourceOriginalOutput()+Constant.Symbol.SLASH_LEFT+"values/"+this.androidConfiguration.getProjectMain()+Constant.Symbol.UNDERLINE+AndroidProject.IDS_XML;
		String originalOutputPublicXmlFullFilename=this.androidConfiguration.getPublicRAndroidProject().getResourceOriginalOutput()+Constant.Symbol.SLASH_LEFT+"values/"+this.androidConfiguration.getProjectMain()+Constant.Symbol.UNDERLINE+AndroidProject.PUBLIC_XML;
		String newOutputIdsXmlFullFilename=this.androidConfiguration.getPublicRAndroidProject().getResourceOutput()+Constant.Symbol.SLASH_LEFT+"values/"+this.androidConfiguration.getProjectMain()+Constant.Symbol.UNDERLINE+AndroidProject.IDS_XML;
		String newOutputPublicXmlFullFilename=this.androidConfiguration.getPublicRAndroidProject().getResourceOutput()+Constant.Symbol.SLASH_LEFT+"values/"+this.androidConfiguration.getProjectMain()+Constant.Symbol.UNDERLINE+AndroidProject.PUBLIC_XML;
		AaptUtil.generatePublicResourceXml(aaptResourceCollector, originalOutputIdsXmlFullFilename, originalOutputPublicXmlFullFilename);
		//copy original id.xml and public.xml to resource output
		FileUtil.copyFile(originalOutputIdsXmlFullFilename, newOutputIdsXmlFullFilename, FileUtil.FileCopyType.FILE_TO_FILE);
		FileUtil.copyFile(originalOutputPublicXmlFullFilename, newOutputPublicXmlFullFilename, FileUtil.FileCopyType.FILE_TO_FILE);
		List<String> tempOriginalResourceDirectoryList=new ArrayList<String>(originalResourceDirectoryList);
		tempOriginalResourceDirectoryList.add(androidConfiguration.getPublicRAndroidProject().getResourceOriginalOutput());
		FileUtil.createDirectory(this.androidConfiguration.getPublicRAndroidProject().getGenOriginalOutput());
		
        Map<String, Map<RType, Set<RDotTxtEntry>>> directoryRTypeResourceMap = aaptResourceCollector.getDirectoryRTypeResourceMap();
        for (AndroidProject androidProject : androidProjectList) {
            List<String> resourceDirectoryList = new ArrayList<String>();
            resourceDirectoryList.addAll(androidProject.getResourceDirectoryList());
            List<Project> parentProjectList = androidProject.getParentProjectList();
            for (Project parentProject : parentProjectList) {
                AndroidProject parentAndroidProject = (AndroidProject) parentProject;
                resourceDirectoryList.addAll(parentAndroidProject.getResourceDirectoryList());
            }
            resourceDirectoryList = filterDuplicateFile(resourceDirectoryList);

            Map<RType, Set<RDotTxtEntry>> allRTypeResourceMap = new HashMap<RType, Set<RDotTxtEntry>>();
            for (String resourceDirectory : resourceDirectoryList) {
                mergeRTypeResourceMap(allRTypeResourceMap, directoryRTypeResourceMap.get(resourceDirectory));
            }
            AaptUtil.writeRJava(androidProject.getGenOutput(), androidProject.getPackageName(), allRTypeResourceMap, false);
        }
		
//		int result=BuilderUtil.executeAndroidAaptToGenerateR(this.android.getAaptExecutor(),this.androidConfiguration.getMainAndroidProject().getAndroidManifestList().get(0),tempOriginalResourceDirectoryList,this.androidConfiguration.getPublicRAndroidProject().getGenOriginalOutput(),Arrays.asList(this.androidConfiguration.getMainAndroidApiJar()),this.androidConfiguration.isApkDebug());
//		logger.log("Aapt generate R result code:"+result);
		//
		return true;
	}

    public static void mergeRTypeResourceMap(Map<RType, Set<RDotTxtEntry>> allRTypeResourceMap, Map<RType, Set<RDotTxtEntry>> rTypeResourceMap) {
        if (allRTypeResourceMap == null || rTypeResourceMap == null) {
            return;
        }
        for (RType rType : rTypeResourceMap.keySet()) {
            Set<RDotTxtEntry> set = null;
            if (allRTypeResourceMap.containsKey(rType)) {
                set = allRTypeResourceMap.get(rType);
            } else {
                set = new HashSet<RDotTxtEntry>();
                allRTypeResourceMap.put(rType, set);
            }
            set.addAll(rTypeResourceMap.get(rType));
        }
    }
}
