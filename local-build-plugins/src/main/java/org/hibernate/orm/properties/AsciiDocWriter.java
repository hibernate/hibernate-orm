/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.properties;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;

import org.gradle.api.Project;
import org.gradle.api.file.RegularFile;

/**
 * @author Marko Bekhta
 */
public class AsciiDocWriter {
	public static final String ANCHOR_BASE = "settings-";
	public static final String ANCHOR_START = "[[" + ANCHOR_BASE;

	public static void writeToFile(
			SortedMap<SettingsDocSection, SortedSet<SettingDescriptor>> settingDescriptorMap,
			RegularFile outputFile,
			Project project) {
		final File outputFileAsFile = outputFile.getAsFile();
		try {
			Files.createDirectories( outputFileAsFile.getParentFile().toPath() );
		}
		catch (IOException e) {
			throw new RuntimeException( "Unable to prepare output directory for writing", e );
		}

		try ( FileWriter fileWriter = new FileWriter( outputFileAsFile ) ) {
			write( settingDescriptorMap, fileWriter, project );
		}
		catch (IOException e) {
			throw new RuntimeException( "Failed to produce asciidoc output for collected properties", e );
		}
	}

	private static void write(
			SortedMap<SettingsDocSection, SortedSet<SettingDescriptor>> settingDescriptorMap,
			FileWriter writer,
			Project project) throws IOException {
		for ( Map.Entry<SettingsDocSection, SortedSet<SettingDescriptor>> entry : settingDescriptorMap.entrySet() ) {
			final SettingsDocSection sectionDescriptor = entry.getKey();
			final SortedSet<SettingDescriptor> sectionSettingDescriptors = entry.getValue();

			final Project sourceProject = project.getRootProject().project( sectionDescriptor.getProjectPath() );

			// write an anchor in the form `[[settings-{moduleName}]]`, e.g. `[[settings-hibernate-core]]`
			tryToWriteLine( writer, ANCHOR_START, sourceProject.getName(), "]]" );
			tryToWriteLine( writer, "=== ", "(", sourceProject.getName(), ") ", sourceProject.getDescription() );

			writer.write( '\n' );

			for ( SettingDescriptor settingDescriptor : sectionSettingDescriptors ) {
				writer.write( ANCHOR_START );
				writer.write( settingDescriptor.getName() );
				writer.write( "]] " );

				writer.write( '`' );
				writer.write( settingDescriptor.getName() );
				writer.write( '`' );
				writer.write( "::\n" );

				writer.write( settingDescriptor.getJavadoc() );

				writer.write( '\n' );

				writer.write(
						String.format(
								"**See:** %s[%s.%s]\n\n",
								settingDescriptor.getJavadocLink(),
								Utils.withoutPackagePrefix( settingDescriptor.getSettingsClassName() ),
								settingDescriptor.getSettingFieldName()
						)
				);

				writer.write( "'''\n" );
			}

			writer.write( '\n' );
		}
	}

	private static void tryToWriteLine(Writer writer, String prefix, String value, String... other) {
		try {
			writer.write( prefix );
			writer.write( value );
			for ( String s : other ) {
				writer.write( s );
			}
			writer.write( "\n" );
		}
		catch (IOException e) {
			throw new RuntimeException( "Unable to create asciidoc output", e );
		}
	}
}
