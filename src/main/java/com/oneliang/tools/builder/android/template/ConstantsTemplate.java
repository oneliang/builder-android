package com.oneliang.tools.builder.android.template;

import java.io.InputStream;

public final class ConstantsTemplate {

	public static enum Template {
		BUILD_CONFIG("BuildConfig.template"), ANDROID_MANIFEST("AndroidManifest.template");
		private String templateFilename = null;

		private Template(String templateFilename) {
			this.templateFilename = templateFilename;
		}

		private String getTemplateFilename() {
			return this.templateFilename;
		}
	}

	public static InputStream getTemplateInputStream(Template template) {
		return ConstantsTemplate.class.getResourceAsStream(template.getTemplateFilename());
	}
}
