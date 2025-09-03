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
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

/**
 * Task to perform the XJC processing using {@linkplain Driver}
 *
 * @author Steve Ebersole
 */
@CacheableTask
public class XjcTask extends DefaultTask {
	private final Property<String> schemaName;
	private final DirectoryProperty outputDirectory;
	private final RegularFileProperty xsdFile;
	private final RegularFileProperty xjcBindingFile;
	private final SetProperty<String> xjcPlugins;

	public XjcTask() {
		schemaName = getProject().getObjects().property( String.class );

		xsdFile = getProject().getObjects().fileProperty();
		xjcBindingFile = getProject().getObjects().fileProperty();
		xjcPlugins = getProject().getObjects().setProperty( String.class );

		outputDirectory = getProject().getObjects().directoryProperty();

		schemaName.convention( xsdFile.map( regularFile -> regularFile.getAsFile().getName() ) );
	}

	@Internal
	public Property<String> getSchemaName() {
		return schemaName;
	}

	@InputFile
	@PathSensitive(PathSensitivity.RELATIVE)
	public RegularFileProperty getXsdFile() {
		return xsdFile;
	}

	@InputFile
	@PathSensitive(PathSensitivity.RELATIVE)
	public RegularFileProperty getXjcBindingFile() {
		return xjcBindingFile;
	}

	@Input
	public SetProperty<String> getXjcPlugins() {
		return xjcPlugins;
	}

	@OutputDirectory
	public DirectoryProperty getOutputDirectory() {
		return outputDirectory;
	}

	@TaskAction
	public void generateJaxbBindings() {
		getProject().delete( outputDirectory.get().getAsFileTree() );

		final XjcListenerImpl listener = new XjcListenerImpl( schemaName.get(), getProject() );
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

		Collections.addAll( argsList, "-d", outputDirectory.get().getAsFile().getAbsolutePath() );
		Collections.addAll( argsList,  "-b", xjcBindingFile.get().getAsFile().getAbsolutePath());

		argsList.add( "-extension" );
		argsList.add( "-no-header" );
		argsList.add( "-npa" );

		if ( xjcPlugins.isPresent() ) {
			final Set<String> xjcPluginsToEnable = xjcPlugins.get();
			if ( !xjcPluginsToEnable.isEmpty() ) {
				xjcPluginsToEnable.forEach( (ext) -> {
					argsList.add( "-X" + ext );
				} );
			}
		}

		argsList.add( xsdFile.get().getAsFile().getAbsolutePath() );

		return argsList.toArray( new String[0] );
	}

}
