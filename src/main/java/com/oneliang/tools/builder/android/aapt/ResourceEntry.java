package com.oneliang.tools.builder.android.aapt;

import java.util.Arrays;

import com.oneliang.util.common.ObjectUtil;

public class ResourceEntry {

	public String name=null;
	public String value=null;

	public ResourceEntry(String name,String value) {
		this.name=name;
		this.value=value;
	}

	public int hashCode() {
		return Arrays.hashCode(new Object[] { this.name });
	}


	public boolean equals(Object object) {
		if(!(object instanceof ResourceEntry)){
			return false;
		}
		ResourceEntry that=(ResourceEntry)object;
		return ObjectUtil.equal(this.name, that.name);
	}
}
