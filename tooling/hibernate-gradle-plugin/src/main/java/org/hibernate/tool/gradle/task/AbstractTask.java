package org.hibernate.tool.gradle.task;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Internal;
import org.hibernate.tool.gradle.Extension;

public abstract class AbstractTask extends DefaultTask {

	@Internal
	private Extension extension = null;
	
	public void initialize(Extension extension) {
		this.extension = extension;
	}
	
	Extension getExtension() {
		return this.extension;
	}

}
