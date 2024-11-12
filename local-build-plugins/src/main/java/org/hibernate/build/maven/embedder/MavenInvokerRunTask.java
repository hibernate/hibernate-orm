package org.hibernate.build.maven.embedder;

import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.TaskAction;

public abstract class MavenInvokerRunTask extends DefaultTask {

	@ServiceReference
	abstract Property<MavenEmbedderService> getMavenEmbedderService();

	@TaskAction
	public void runInvoker() {
		getMavenEmbedderService().get().execute( "invoker:run");
	}

}
