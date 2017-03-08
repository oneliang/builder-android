package com.oneliang.tools.builder.android.handler;

import java.util.ArrayList;
import java.util.List;

import com.oneliang.Constant;
import com.oneliang.tools.builder.android.base.AndroidProject;
import com.oneliang.tools.builder.base.BuilderUtil;
import com.oneliang.util.common.StringUtil;
import com.oneliang.util.file.FileUtil;

public class MergeDexHandler extends MultiAndroidProjectDexHandler {

	public boolean handle() {
		String dexFullFilename=null;
		String mergeDexOutputDirectory=null;
		boolean isAllCompileFileHasCache=this.isAllCompileFileHasCache(androidProjectList);
		if(!isAllCompileFileHasCache||!this.androidConfiguration.isApkDebug()){
			mergeDexOutputDirectory=this.androidConfiguration.getMainAndroidProject().getMergeDexOutput();
			FileUtil.createDirectory(mergeDexOutputDirectory);
			dexFullFilename=mergeDexOutputDirectory+Constant.Symbol.SLASH_LEFT+AndroidProject.CLASSES+(dexId==0?StringUtil.BLANK:dexId+1)+Constant.Symbol.DOT+Constant.File.DEX;
			List<String> toMergeDexFullFilenameList=new ArrayList<String>();
			for(AndroidProject androidProject:androidProjectList){
				String androidProjectDexFullFilename=androidProject.getDexOutput()+"/"+androidProject.getName()+Constant.Symbol.DOT+Constant.File.DEX;
				if(FileUtil.isExist(androidProjectDexFullFilename)){
					toMergeDexFullFilenameList.add(androidProjectDexFullFilename);
				}
			}
			try{
				BuilderUtil.androidMergeDex(dexFullFilename,toMergeDexFullFilenameList);
			}catch(Exception e){
				logger.error(Constant.Base.EXCEPTION+",dexId:"+dexId,e);
				return false;
			}
		}
		return true;
	}
}
