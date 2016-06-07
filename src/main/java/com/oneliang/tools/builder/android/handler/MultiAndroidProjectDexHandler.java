package com.oneliang.tools.builder.android.handler;

import java.util.List;

import com.oneliang.tools.builder.android.base.AndroidProject;
import com.oneliang.tools.builder.base.Handler;

public class MultiAndroidProjectDexHandler extends AbstractAndroidHandler {

	protected int dexId=0;
	protected List<AndroidProject> androidProjectList=null;

	protected void beforeInnerHandlerHandle(Handler handler) {
		super.beforeInnerHandlerHandle(handler);
		if(handler!=null&&handler instanceof MultiAndroidProjectDexHandler){
			MultiAndroidProjectDexHandler multiAndroidProjectDexHandler=(MultiAndroidProjectDexHandler)handler;
			multiAndroidProjectDexHandler.setDexId(dexId);
			multiAndroidProjectDexHandler.setAndroidProjectList(androidProjectList);
		}
	}

	/**
	 * @param dexId the dexId to set
	 */
	public void setDexId(int dexId) {
		this.dexId = dexId;
	}

	/**
	 * @param androidProjectList the androidProjectList to set
	 */
	public void setAndroidProjectList(List<AndroidProject> androidProjectList) {
		this.androidProjectList = androidProjectList;
	}
}
