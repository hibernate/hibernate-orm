/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.properties.jdk11;

import java.io.File;
import java.io.IOException;
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
import static org.hibernate.orm.properties.Utils.packagePrefix;
import static org.hibernate.orm.properties.Utils.withoutPackagePrefix;


/**
 * Processes Javadoc into SettingDescriptor based on the JDK 11 javadoc format
 *
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
		final Document constantValuesDocument = loadConstants( javadocDirectory );

		// For each <table class="constantsSummary"/> Element in constant-values.html...
		for ( Element table : constantValuesDocument.select( "table.constantsSummary" ) ) {
			// e.g. org.hibernate.cfg.JdbcSettings
			final String className = table.selectFirst( "caption" ).text();

			// find the doc section descriptor defined for this class, if one
			final SettingsDocSection docSection = findMatchingDocSection( className, sections );
			if ( docSection == null ) {
				// does not match any defined sections, skip it
				continue;
			}

			// todo (settings-doc) : consider extracting all @see tags and grabbing ones that refer to other "setting field"
			//				and ultimately render "cross links" - i.e. `@see JdbcSettings#JAKARTA_JDBC_URL`.
			//		- or even handling all "setting constant" links this way
			//		- these are contained as notes in the Javadoc.
			//		- this would require a second pass though after all "setting details" have bee processed.
			//		- we don't need this until ^^
			//final Map<String, SettingWorkingDetails> settingWorkingDetailsMap = new HashMap<>();

			// Load the Javadoc HTML for the constants class, e.g. org.hibernate.cfg.JdbcSettings
			final Document constantsClassDocument = loadClassJavadoc( className, javadocDirectory );

			// go through constants from constant-values.html
			for ( Element row : table.select( "tr" ) ) {
				if ( row.hasClass( "altColor" ) || row.hasClass( "rowColor" ) ) {
					// <td class="colFirst">
					// 	  <a id="org.hibernate.cfg.JdbcSettings.DIALECT">
					final String constantFieldFqn  = row.selectFirst( ".colFirst a" ).id();
					final String simpleFieldName = constantFieldFqn.substring( constantFieldFqn.lastIndexOf( '.' ) + 1 );

					// <td class="colLast"><code>"hibernate.dialect"</code></td>
					final String constantValue  = row.selectFirst( ".colLast" ).text();
					final String settingName = stripQuotes( constantValue );

					// locate the blockList for the field from the constants class Javadoc
					//
					// <a id="DIALECT">
					//   <!--   -->
					// </a>
					// <ul class="blockList">
					//   <li class="blockList">
					//     <h4>DIALECT</h4>
					//     ..
					//     <div class="block">{COMMENT}</div>
					//     <dl>
					//       <!-- "notes" -->
					//     </dl>
					//   </li>
					// </ul>
					final Element fieldJavadocBlockList = constantsClassDocument.selectFirst( "#" + simpleFieldName + " + ul li.blockList" );

					if ( fieldJavadocBlockList == null || isUselessJavadoc( fieldJavadocBlockList ) ) {
						System.out.println( className + "#" + simpleFieldName + " defined no Javadoc - ignoring." );
						continue;
					}

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

					applyMetadata( className, publishedJavadocsUrl, settingWorkingDetails, fieldJavadocBlockList );

					final Elements javadocsToConvert = cleanupFieldJavadocElement( fieldJavadocBlockList, className, publishedJavadocsUrl );
					final SettingDescriptor settingDescriptor = settingWorkingDetails.buildDescriptor(
							DomToAsciidocConverter.convert( javadocsToConvert )
					);

					final SortedSet<SettingDescriptor> docSectionSettings = result.get( docSection );
					docSectionSettings.add( settingDescriptor );
				}
			}
		}

		return result;
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

	/**
	 * @param fieldJavadocElement The element to inspect if it has some valuable documentation.
	 * @return {@code true} if no Javadoc was written for this field; {@code false} otherwise.
	 */
	private static boolean isUselessJavadoc(Element fieldJavadocElement) {
		return fieldJavadocElement.selectFirst( "div.block" ) == null;
	}

	private static void applyMetadata(
			String className,
			String publishedJavadocsUrl,
			SettingWorkingDetails settingDetails,
			Element fieldJavadocElement) {
		processNotes(
				fieldJavadocElement,
				(name, content) -> {
					switch ( name ) {
						case "Default Value:": {
							fixUrls( className, publishedJavadocsUrl, content );
							final String defaultValueText = DomToAsciidocConverter.convert( content );
							settingDetails.setDefaultValue( defaultValueText );
							break;
						}
						case "API Note:": {
							settingDetails.setApiNote( content.text() );
							break;
						}
						case "Since:": {
							settingDetails.setSince( content.text() );
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

	private static String stripQuotes(String value) {
		if ( value.startsWith( "\"" ) && value.endsWith( "\"" ) ) {
			return value.substring( 1, value.length() - 1 );
		}
		return value;
	}

	@FunctionalInterface
	private interface NoteConsumer {
		void consumeNote(String name, Element content);
	}

	private static void processNotes(
			Element fieldJavadocElement,
			NoteConsumer noteConsumer) {
		final Element notesElement = fieldJavadocElement.selectFirst( "dl" );
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

			noteConsumer.consumeNote( dtNode.text().trim(), ddNode );
		}
	}

	private static Elements cleanupFieldJavadocElement(
			Element fieldJavadocElement,
			String className,
			String publishedJavadocsUrl) {
		// Before proceeding let's make sure that the javadoc structure is the one that we expect:
		if ( !isValidFieldJavadocStructure( fieldJavadocElement ) ) {
			throw new IllegalStateException(
					"Javadoc's DOM doesn't match the expected structure. " +
							"This may lead to unexpected results in rendered configuration properties in the User Guide." 
			);
		}

		fixUrls( className, publishedJavadocsUrl, fieldJavadocElement );

		final Elements usefulDocsPart = new Elements();

		// We want to take the javadoc comment block...
		final Element javadocComment = fieldJavadocElement.selectFirst( "div.block" );
		usefulDocsPart.add( javadocComment );

		return usefulDocsPart;
	}

	/**
	 * Any ORM links will be relative, and we want to prepend them with the {@code publishedJavadocsUrl}
	 * so that they are usable when the doc is published.
	 * Any external links should have a {@code externalLink} or {@code external-link} CSS class, we want to keep them unchanged.
	 * <p>
	 * NOTE: this method modifies the parsed DOM.
	 */
	private static void fixUrls(
			String className,
			String publishedJavadocsUrl,
			Element fieldJavadocElement) {
		for ( Element link : fieldJavadocElement.getElementsByTag( "a" ) ) {
			// only update links if they are not external:
			if ( !isExternalLink( link ) ) {
				String href = link.attr( "href" );
				if ( href.startsWith( "#" ) ) {
					href = withoutPackagePrefix( className ) + ".html" + href;
				}
				String packagePath = packagePrefix( className ).replace( ".", File.separator );
				href = publishedJavadocsUrl + packagePath + "/" + href;
				link.attr( "href", href );
			}
		}
	}

	private static boolean isExternalLink(Element link) {
		String href = link.attr( "href" );
		return link.hasClass( "externalLink" )
				|| link.hasClass( "external-link" )
				|| href != null && href.startsWith( "http" );
	}

	private static boolean isValidFieldJavadocStructure(Element fieldJavadocElement) {
		// Field's DOM sub-structure that we are expecting should be:
		//
		// <ul class="blockList">
		//   <li class="blockList">
		//     <h4>DIALECT</h4>
		//     <pre>
		//         <!-- might have @Incubating, @Deprecated or @Remove 
		//     </pre>
		//     ...
		//     <div class="block">{COMMENT}</div>
		//     <dl>
		//       <!-- "notes" -->
		//     </dl>
		//   </li>
		// </ul>
		
		if ( !"li".equals( fieldJavadocElement.tagName() ) ) {
			return false;
		}
		
		if ( !fieldJavadocElement.hasClass( "blockList" ) ) {
			return false;
		}

		if ( fieldJavadocElement.selectFirst( "div.block" ) == null
				&& fieldJavadocElement.selectFirst( "dl" ) == null
				&& fieldJavadocElement.selectFirst( "pre" ) == null ) {
			return false;
		}

		return true;
	}

}
