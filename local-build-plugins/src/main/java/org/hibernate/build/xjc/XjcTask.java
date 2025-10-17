/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.build.xjc;

import com.sun.tools.xjc.BadCommandLineException;
import com.sun.tools.xjc.Driver;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

/**
 * Task to perform the XJC processing using {@linkplain Driver}
 *
 * @author Steve Ebersole
 */
@CacheableTask
public abstract class XjcTask extends DefaultTask {

	@Inject
	public XjcTask() {
	}

	@Internal
	public abstract Property<String> getSchemaName();

	@InputFile
	@PathSensitive(PathSensitivity.RELATIVE)
	public abstract RegularFileProperty getXsdFile();

	@Optional
	@InputFile
	@PathSensitive(PathSensitivity.RELATIVE)
	public abstract RegularFileProperty getXjcBindingFile();

	@Input
	public abstract SetProperty<String> getXjcPlugins();

	@OutputDirectory
	public abstract DirectoryProperty getOutputDirectory();

	@TaskAction
	public void generateJaxbBindings() {
		final XjcListenerImpl listener = new XjcListenerImpl( getSchemaName().get(), getLogger() );
		final String[] args = buildXjcArgs();

		try {
			Driver.run( args, listener );
		}
		catch (BadCommandLineException e) {
			throw new RuntimeException( "Error running XJC process", e );
		}
	}

	private String[] buildXjcArgs() {
		final ArrayList<String> argsList = new ArrayList<>();

		Collections.addAll( argsList, "-d", getOutputDirectory().get().getAsFile().getAbsolutePath() );
		Collections.addAll( argsList,  "-b", getXjcBindingFile().get().getAsFile().getAbsolutePath());

		argsList.add( "-extension" );
		argsList.add( "-no-header" );
		argsList.add( "-npa" );

		if ( getXjcPlugins().isPresent() ) {
			final Set<String> xjcPluginsToEnable = getXjcPlugins().get();
			if ( !xjcPluginsToEnable.isEmpty() ) {
				xjcPluginsToEnable.forEach( (ext) -> {
					argsList.add( "-X" + ext );
				} );
			}
		}

		argsList.add( getXsdFile().get().getAsFile().getAbsolutePath() );

		return argsList.toArray( new String[0] );
	}

}
