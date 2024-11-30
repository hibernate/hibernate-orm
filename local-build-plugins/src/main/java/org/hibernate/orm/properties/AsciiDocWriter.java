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
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;

import org.gradle.api.Project;
import org.gradle.api.file.RegularFile;

/**
 * @author Marko Bekhta
 */
public class AsciiDocWriter {

	public static void writeToFile(
			String anchorNameBase,
			Map<SettingsDocSection, SortedSet<SettingDescriptor>> settingDescriptorMap,
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
			write( anchorNameBase, settingDescriptorMap, fileWriter, project );
		}
		catch (IOException e) {
			throw new RuntimeException( "Failed to produce asciidoc output for collected properties", e );
		}
	}

	private static void write(
			String anchorNameBase,
			Map<SettingsDocSection, SortedSet<SettingDescriptor>> settingDescriptorMap,
			FileWriter writer,
			Project project) throws IOException {
		for ( Map.Entry<SettingsDocSection, SortedSet<SettingDescriptor>> entry : settingDescriptorMap.entrySet() ) {
			final SettingsDocSection sectionDescriptor = entry.getKey();
			final SortedSet<SettingDescriptor> sectionSettingDescriptors = entry.getValue();
			if ( sectionSettingDescriptors.isEmpty() ) {
				continue;
			}

			final String sectionName = sectionDescriptor.getName();

			// write an anchor in the form `[[{anchorNameBase}-{sectionName}]]`
			tryToWriteLine( writer, "[[", anchorNameBase, "-", sectionName, "]]" );
			tryToWriteLine( writer, "=== ", sectionDescriptor.getSummary() );

			writer.write( '\n' );

			for ( SettingDescriptor settingDescriptor : sectionSettingDescriptors ) {
				// write an anchor in the form `[[{anchorNameBase}-{settingName}]]`
				tryToWriteLine( writer, "[[", anchorNameBase, "-", settingDescriptor.getName(), "]]" );
				tryToWriteLine( writer, "==== ", settingName( settingDescriptor ) );

				writeMetadata( settingDescriptor, writer );

				writer.write( settingDescriptor.getComment() );

				writer.write( "\n\n'''\n" );
			}

			writer.write( '\n' );
		}
	}

	private static void writeMetadata(SettingDescriptor settingDescriptor, FileWriter writer) throws IOException {
		if ( !settingDescriptor.hasMetadata() ) {
			return;
		}

		writer.write( "****\n" );

		writer.write(
				String.format(
						Locale.ROOT,
						"**See:** %s[%s.%s]\n\n",
						settingDescriptor.getPublishedJavadocLink(),
						Utils.withoutPackagePrefix( settingDescriptor.getSettingsClassName() ),
						settingDescriptor.getSettingFieldName()
				)
		);

		// NOTE : Asciidoctor requires that italic always be the innermost formatting

		final SettingDescriptor.LifecycleDetails lifecycleDetails = settingDescriptor.getLifecycleDetails();

		// NOTE : at the moment, there is at least one setting that is incubating AND deprecated which fundamentally seems wrong
		if ( lifecycleDetails.isIncubating() ) {
			writer.write( "NOTE: *_This setting is considered incubating_*\n\n" );
		}
		if ( lifecycleDetails.isDeprecated() ) {
			writer.write( "WARNING: *_This setting is considered deprecated_*\n\n" );
		}
		if ( settingDescriptor.isUnsafe() ) {
			writer.write( "WARNING: *_This setting is considered unsafe_*\n\n" );
		}
		if ( settingDescriptor.isCompatibility() ) {
			writer.write( "INFO: *_This setting manages a certain backwards compatibility_*\n\n" );
		}

		if ( lifecycleDetails.getSince() != null ) {
			writer.write( "*_Since:_* _" + lifecycleDetails.getSince() + "_\n\n" );
		}

		if ( settingDescriptor.getDefaultValue() != null ) {
			writer.write( "*_Default Value:_* " + settingDescriptor.getDefaultValue() + "\n\n" );
		}

		if ( settingDescriptor.getApiNote() != null ) {
			writer.write( settingDescriptor.getApiNote() + "\n\n" );
		}

		writer.write( "****\n\n" );
	}

	private static String settingName(SettingDescriptor settingDescriptor) {
		if ( settingDescriptor.getLifecycleDetails().isDeprecated() ) {
			return String.format(
					Locale.ROOT,
					"`[.line-through]#%s#`",
					settingDescriptor.getName()
			);
		}
		else {
			return String.format(
					Locale.ROOT,
					"`%s`",
					settingDescriptor.getName()
			);
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
