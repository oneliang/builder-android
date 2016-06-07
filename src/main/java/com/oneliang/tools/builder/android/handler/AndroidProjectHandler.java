package com.oneliang.tools.builder.android.handler;

import com.oneliang.tools.builder.android.base.AndroidProject;
import com.oneliang.tools.builder.base.Handler;

public class AndroidProjectHandler extends AbstractAndroidHandler {

	protected AndroidProject androidProject=null;

	protected void beforeInnerHandlerHandle(Handler handler) {
		if(handler!=null&&handler instanceof AndroidProjectHandler){
			AndroidProjectHandler androidProjectHandler=(AndroidProjectHandler)handler;
			androidProjectHandler.setAndroidProject(this.androidProject);
		}
	}

	/**
	 * @param androidProject the androidProject to set
	 */
	public void setAndroidProject(AndroidProject androidProject) {
		this.androidProject = androidProject;
	}
}
