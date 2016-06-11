package com.oneliang.tools.builder.android.handler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.oneliang.tools.builder.android.aapt.AaptUtil;
import com.oneliang.tools.builder.android.aapt.RDotTxtEntry;
import com.oneliang.tools.builder.android.aapt.RDotTxtEntry.RType;
import com.oneliang.tools.builder.android.base.AndroidProject;
import com.oneliang.tools.builder.base.Project;

public class GenerateAndroidProjectRFileHandler extends AndroidProjectHandler {

	public boolean handle() {
		if(!this.androidConfiguration.isAaptGenerateRFile()){
			try{
				this.generateAndroidProjectRFile(androidProject);
			}catch(Exception e){
				logger.error(androidProject.getName()+" mini aapt generate R.java file failure", e);
				return false;
			}
		}		
		return true;
	}

	/**
	 * generate android project r file
	 * @param androidProject
	 */
	protected void generateAndroidProjectRFile(AndroidProject androidProject) {
		//find parent android project reference resource
		Map<RType, Set<RDotTxtEntry>> allParentAndroidProjectRTypeResourceMap=new HashMap<RType, Set<RDotTxtEntry>>();
		if(androidProject.getParentProjectList()!=null){
			for(Project parentProject:androidProject.getParentProjectList()){
				if(parentProject!=null&&(parentProject instanceof AndroidProject)){
					AndroidProject parentAndroidProject=(AndroidProject)parentProject;
					Map<RType, Set<RDotTxtEntry>> parentAndroidProjectRTypeResourceMap=parentAndroidProject.getRTypeResourceMap();
					this.mergeRTypeResourceMap(allParentAndroidProjectRTypeResourceMap, parentAndroidProjectRTypeResourceMap);
				}
			}
		}
		//collect current android project resource
		Map<RType, Set<RDotTxtEntry>> rTypeResourceMap=AaptUtil.collectResource(androidProject.getResourceDirectoryList(), allParentAndroidProjectRTypeResourceMap).getRTypeResourceMap();
		Map<RType, Set<RDotTxtEntry>> allRTypeResourceMap=new HashMap<RType, Set<RDotTxtEntry>>();
		this.mergeRTypeResourceMap(allRTypeResourceMap, allParentAndroidProjectRTypeResourceMap);
		this.mergeRTypeResourceMap(allRTypeResourceMap, rTypeResourceMap);
		androidProject.setRTypeResourceMap(allRTypeResourceMap);
		
		
//		List<PackageRTypeResourceMap> packageRTypeResourceMapList=new ArrayList<PackageRTypeResourceMap>();
//		List<AndroidProject> parentAndSelfAndroidProjectList=new ArrayList<AndroidProject>();
//		parentAndSelfAndroidProjectList.add(androidProject);
//		for(Project parentProject:androidProject.getParentProjectList()){
//			if(parentProject!=null&&(parentProject instanceof AndroidProject)){
//				AndroidProject parentAndroidProject=(AndroidProject)parentProject;
//				parentAndSelfAndroidProjectList.add(parentAndroidProject);
//			}
//		}
//		for(AndroidProject parentAndSelfAndroidProject:parentAndSelfAndroidProjectList){
//			packageRTypeResourceMapList.add(new PackageRTypeResourceMap(parentAndSelfAndroidProject.getPackageName(),parentAndSelfAndroidProject.getRTypeResourceMap()));
//		}
//		Map<String, Map<RType,Set<RDotTxtEntry>>> packageRTypeResourceMap=AaptUtil.mergePackageRTypeResourceMap(packageRTypeResourceMapList);
		Map<String, Map<RType,Set<RDotTxtEntry>>> packageRTypeResourceMap=new HashMap<String, Map<RType,Set<RDotTxtEntry>>>();
		packageRTypeResourceMap.put(this.androidProject.getPackageName(), allRTypeResourceMap);
		String projectGenOutput=androidProject.getGenOutput();
		AaptUtil.writeRJava(projectGenOutput, packageRTypeResourceMap, false);
//		BuilderUtil.executeAndroidAaptToGenerateR(this.android.getAaptExecutor(),projectAndroidManifest,resDirectoryList,projectGenOutput,this.configuration.getMainAndroidApiJar(),this.configuration.isApkDebug());
	}

	private void mergeRTypeResourceMap(Map<RType, Set<RDotTxtEntry>> allRTypeResourceMap,Map<RType, Set<RDotTxtEntry>> rTypeResourceMap){
		if(allRTypeResourceMap!=null&&rTypeResourceMap!=null){
			for(RType rType:rTypeResourceMap.keySet()){
				Set<RDotTxtEntry> set=null;
				if(allRTypeResourceMap.containsKey(rType)){
					set=allRTypeResourceMap.get(rType);
				}else{
					set=new HashSet<RDotTxtEntry>();
					allRTypeResourceMap.put(rType, set);
				}
				set.addAll(rTypeResourceMap.get(rType));
			}
		}
	}
}
