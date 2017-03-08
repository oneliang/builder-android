package com.oneliang.tools.builder.android.handler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.oneliang.tools.builder.android.base.AndroidProject;
import com.oneliang.tools.linearalloc.LinearAllocUtil;
import com.oneliang.tools.linearalloc.LinearAllocUtil.AllocStat;

public class EstimateLinearAllocHandler extends AbstractAndroidHandler {

	public boolean handle() {
		if(!isAllCompileFileHasCache(this.androidConfiguration.getAndroidProjectList())){
			List<AndroidProject> androidProjectList=this.androidConfiguration.getAndroidProjectList();
			Map<Integer,AllocStat> dexAllocStatMap=new HashMap<Integer,AllocStat>();
			for(final AndroidProject androidProject:androidProjectList){
				List<String> androidProjectJarList=this.getAndroidProjectJarListWithProguard(androidProject);
				if(androidProjectJarList!=null&&!androidProjectJarList.isEmpty()){
					AllocStat androidProjectAllocStat=new AllocStat();
					androidProjectAllocStat.setFieldReferenceMap(new HashMap<String,String>());
					androidProjectAllocStat.setMethodReferenceMap(new HashMap<String,String>());
					for(String androidProjectJar:androidProjectJarList){
						AllocStat allocStat=LinearAllocUtil.estimateJar(androidProjectJar);
						androidProjectAllocStat.setTotalAlloc(androidProjectAllocStat.getTotalAlloc()+allocStat.getTotalAlloc());
						androidProjectAllocStat.getFieldReferenceMap().putAll(allocStat.getFieldReferenceMap());
						androidProjectAllocStat.getMethodReferenceMap().putAll(allocStat.getMethodReferenceMap());
						System.out.println("\t"+androidProjectJar+"\t"+allocStat.getTotalAlloc()+"\t"+allocStat.getFieldReferenceMap().size()+"\t"+allocStat.getMethodReferenceMap().size());
					}
					System.out.println("\t"+androidProject.getName()+"\t"+androidProjectAllocStat.getTotalAlloc()+"\t"+androidProjectAllocStat.getFieldReferenceMap().size()+"\t"+androidProjectAllocStat.getMethodReferenceMap().size());
					int dexId=androidProject.getDexId();
					AllocStat dexAllocStat=null;
					if(!dexAllocStatMap.containsKey(dexId)){
						dexAllocStat=new AllocStat();
						dexAllocStat.setFieldReferenceMap(new HashMap<String,String>());
						dexAllocStat.setMethodReferenceMap(new HashMap<String,String>());
						dexAllocStatMap.put(dexId, dexAllocStat);
					}else{
						dexAllocStat=dexAllocStatMap.get(dexId);
					}
					dexAllocStat.setTotalAlloc(dexAllocStat.getTotalAlloc()+androidProjectAllocStat.getTotalAlloc());
					dexAllocStat.getFieldReferenceMap().putAll(androidProjectAllocStat.getFieldReferenceMap());
					dexAllocStat.getMethodReferenceMap().putAll(androidProjectAllocStat.getMethodReferenceMap());
				}
			}
			Iterator<Entry<Integer,AllocStat>> iterator=dexAllocStatMap.entrySet().iterator();
			while(iterator.hasNext()){
				Entry<Integer,AllocStat> entry=iterator.next();
				int dexId=entry.getKey();
				AllocStat allocStat=entry.getValue();
				System.out.println("\tdexId:"+dexId+"\t"+allocStat.getTotalAlloc()+"\t"+allocStat.getMethodReferenceMap().size());
			}
		}
		return true;
	}
}
