package com.oneliang.tools.builder.android.aapt;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.oneliang.util.common.ObjectUtil;

public class ResourceDirectory {

	public String directoryName=null;
	public String resourceFullFilename=null;
	public Set<ResourceEntry> resourceEntrySet=new HashSet<ResourceEntry>();

	public ResourceDirectory(String directoryName,String resourceFullFilename) {
		this.directoryName=directoryName;
		this.resourceFullFilename=resourceFullFilename;
	}

	public int hashCode() {
		return Arrays.hashCode(new Object[] { this.directoryName, this.resourceFullFilename });
	}


	public boolean equals(Object object) {
		if(!(object instanceof ResourceDirectory)){
			return false;
		}
		ResourceDirectory that=(ResourceDirectory)object;
		return ObjectUtil.equal(this.directoryName, that.directoryName) && ObjectUtil.equal(this.resourceFullFilename, that.resourceFullFilename);
	}
}
