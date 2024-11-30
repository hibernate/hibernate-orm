/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.properties.jdk17;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

import org.gradle.api.file.Directory;

import org.hibernate.orm.properties.SettingDescriptor;
import org.hibernate.orm.properties.SettingWorkingDetails;
import org.hibernate.orm.properties.SettingsDocSection;
import org.hibernate.orm.properties.Utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import static org.hibernate.orm.properties.Utils.interpretCompatibility;
import static org.hibernate.orm.properties.Utils.interpretDeprecation;
import static org.hibernate.orm.properties.Utils.interpretIncubation;
import static org.hibernate.orm.properties.Utils.interpretUnsafe;
import static org.hibernate.orm.properties.jdk17.JavadocToAsciidocConverter.convertFieldJavadocHtmlToAsciidoc;

/**
 * @author Marko Bekhta
 * @author Steve Ebersole
 */
public class SettingsCollector {

	public static Map<SettingsDocSection, SortedSet<SettingDescriptor>> collectSettingDescriptors(
			Directory javadocDirectory,
			Map<String, SettingsDocSection> sections,
			String publishedJavadocsUrl) {
		return collectSettingDescriptors( javadocDirectory.getAsFile(), sections, publishedJavadocsUrl );
	}

	public static Map<SettingsDocSection, SortedSet<SettingDescriptor>> collectSettingDescriptors(
			File javadocDirectory,
			Map<String, SettingsDocSection> sections,
			String publishedJavadocsUrl) {
		final Map<SettingsDocSection, SortedSet<SettingDescriptor>> result = Utils.createResultMap( sections );

		// Load the constant-values.html file with Jsoup and start processing it
		final Document constantValuesJson = loadConstants( javadocDirectory );

		final Elements captionClassDivs = constantValuesJson.select( "div.caption" );
		for ( Element captionClassDiv : captionClassDivs ) {
			final String className = captionClassDiv.selectFirst( "span" ).text();

			// find the doc section descriptor defined for this class, if one
			final SettingsDocSection docSection = findMatchingDocSection( className, sections );
			if ( docSection == null ) {
				// does not match any defined sections, skip it
				continue;
			}

			// find the summary-table div that contains the constant->value mappings for class-name
			final Element captionClassDivParent = captionClassDiv.parent();
			final Element tableDiv = captionClassDivParent.selectFirst( ".summary-table" );
			final Elements constantFqnColumns = tableDiv.select( ".col-first" );
			final Elements constantValueColumns = tableDiv.select( ".col-last" );

			// extract the Javadoc elements for each field on class-name
			final Map<String, Element> classFieldJavadocs = extractClassFieldJavadocs( className, javadocDirectory );

			// todo (settings-doc) : consider extracting all @see tags and grabbing ones that refer to other "setting field"
			//				and ultimately render "cross links" - i.e. `@see JdbcSettings#JAKARTA_JDBC_URL`.
			//		these are contained as notes in the Javadoc.
			//		this would require a second pass though after all "setting details" have bee processed.
			//		for now, we don't need this
			//final Map<String, SettingWorkingDetails> settingWorkingDetailsMap = new HashMap<>();

			for ( int c = 0; c < constantFqnColumns.size(); c++ ) {
				final Element constantFqnColumn = constantFqnColumns.get( c );
				if ( constantFqnColumn.hasClass( "table-header" ) ) {
					continue;
				}

				final String constantFqn = constantFqnColumn.selectFirst( "code" ).id();
				final String constantValue = constantValueColumns.get( c ).selectFirst( "code" ).text();

				// locate the field javadoc from `classFieldJavadocs`.
				// that map is keyed by the simple name of the field, so strip the
				// package and class name from `constantFqn` to do the look-up
				//
				// NOTE : there may be no Javadoc, in which case the Element will be null;
				// there is literally no such div in these cases
				final String simpleFieldName = constantFqn.substring( constantFqn.lastIndexOf( '.' ) + 1 );
				final Element fieldJavadocElement = classFieldJavadocs.get( simpleFieldName );

				if ( fieldJavadocElement == null || isUselessJavadoc( fieldJavadocElement ) ) {
					System.out.println( className + "#" + simpleFieldName + " defined no Javadoc - ignoring." );
					continue;
				}

				final String settingName = stripQuotes( constantValue );
				final SettingWorkingDetails settingWorkingDetails = new SettingWorkingDetails(
						settingName,
						className,
						simpleFieldName,
						Utils.fieldJavadocLink(
								publishedJavadocsUrl,
								className,
								simpleFieldName
						)
				);
				applyMetadata( settingWorkingDetails, fieldJavadocElement );
				// todo (settings-doc) : here is where we'd add to the `settingWorkingDetailsMap`
				//settingWorkingDetailsMap.put( .., settingWorkingDetails );
				// for now though, just generate the SettingDescriptor
				final SettingDescriptor settingDescriptor = settingWorkingDetails.buildDescriptor(
						convertFieldJavadocHtmlToAsciidoc(
								fieldJavadocElement,
								className,
								simpleFieldName,
								publishedJavadocsUrl
						)
				);

				final SortedSet<SettingDescriptor> docSectionSettings = result.get( docSection );
				docSectionSettings.add( settingDescriptor );
			}
		}

		return result;
	}

	/**
	 * @param fieldJavadocElement The element to inspect if it has some valuable documentation.
	 * @return {@code true} if no Javadoc was written for this field; {@code false} otherwise.
	 */
	private static boolean isUselessJavadoc(Element fieldJavadocElement) {
		return fieldJavadocElement.selectFirst( "div.block" ) == null;
	}

	private static void applyMetadata(SettingWorkingDetails settingDetails, Element fieldJavadocElement) {
		processNotes(
				fieldJavadocElement,
				(name, content) -> {
					switch ( name ) {
						case "Default Value:": {
							settingDetails.setDefaultValue( content );
							break;
						}
						case "API Note:": {
							settingDetails.setApiNote( content );
							break;
						}
						case "Since:": {
							settingDetails.setSince( content );
							break;
						}
					}
				}
		);
		settingDetails.setIncubating( interpretIncubation( fieldJavadocElement ) );
		settingDetails.setDeprecated( interpretDeprecation( fieldJavadocElement ) );
		settingDetails.setUnsafe( interpretUnsafe( fieldJavadocElement ) );
		settingDetails.setCompatibility( interpretCompatibility( fieldJavadocElement ) );
	}

	public static Document loadConstants(File javadocDirectory) {
		try {
			final File constantValuesFile = new File( javadocDirectory, "constant-values.html" );
			return Jsoup.parse( constantValuesFile );
		}
		catch (IOException e) {
			throw new IllegalStateException( "Unable to access javadocs `constant-values.html`", e );
		}
	}

	private static SettingsDocSection findMatchingDocSection(
			String className,
			Map<String, SettingsDocSection> sections) {
		for ( Map.Entry<String, SettingsDocSection> entry : sections.entrySet() ) {
			if ( entry.getValue().getSettingsClassNames().contains( className ) ) {
				return entry.getValue();
			}
		}
		return null;
	}

	private static Map<String, Element> extractClassFieldJavadocs(String className, File javadocDirectory) {
		final Map<String, Element> result = new HashMap<>();

		final Document document = loadClassJavadoc( className, javadocDirectory );
		final Elements fieldDetailSections = document.select( "section.detail" );
		for ( Element fieldDetailSection : fieldDetailSections ) {
			final String fieldName = fieldDetailSection.id();
			result.put( fieldName, fieldDetailSection );
		}

		return result;
	}

	private static Document loadClassJavadoc(String enclosingClass, File javadocDirectory) {
		final String classJavadocFileName = enclosingClass.replace( ".", File.separator ) + ".html";
		final File classJavadocFile = new File( javadocDirectory, classJavadocFileName );

		try {
			return Jsoup.parse( classJavadocFile );
		}
		catch (IOException e) {
			throw new RuntimeException( "Unable to access javadocs for " + enclosingClass, e );
		}
	}

	private static String stripQuotes(String value) {
		if ( value.startsWith( "\"" ) && value.endsWith( "\"" ) ) {
			return value.substring( 1, value.length() - 1 );
		}
		return value;
	}

	@FunctionalInterface
	private interface NoteConsumer {
		void consumeNote(String name, String content);
	}

	private static void processNotes(
			Element fieldJavadocElement,
			NoteConsumer noteConsumer) {
		final Element notesElement = fieldJavadocElement.selectFirst( "dl.notes" );
		if ( notesElement == null ) {
			return;
		}

		// should be a list of alternating <dt/> and <dd/> elements.
		final Elements notesChildren = notesElement.children();
		for ( int i = 0; i < notesChildren.size(); i+=2 ) {
			final Element dtNode = notesChildren.get( i );
			if ( !dtNode.tagName().equals( "dt" ) ) {
				throw new RuntimeException( "Unexpected Javadoc format" );
			}

			final Element ddNode = notesChildren.get( i+1 );
			if ( !ddNode.tagName().equals( "dd" ) ) {
				throw new RuntimeException( "Unexpected Javadoc format" );
			}

			noteConsumer.consumeNote( dtNode.text().trim(), ddNode.text().trim() );
		}
	}
}
