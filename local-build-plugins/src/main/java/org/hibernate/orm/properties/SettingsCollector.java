/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.properties;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFile;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

/**
 * @author Marko Bekhta
 * @author Steve Ebersole
 */
public class SettingsCollector {

	public static SortedMap<SettingsDocSection, SortedSet<SettingDescriptor>> collectSettingDescriptors(
			Directory javadocDirectory,
			Map<String, SettingsDocSection> sections,
			String publishedJavadocsUrl) {
		final SortedMap<SettingsDocSection, SortedSet<SettingDescriptor>> result = new TreeMap<>( SettingsDocSection.BY_NAME );

		// Load the constant-values.html file with Jsoup and start processing it
		final Document constantValuesJson = loadConstants( javadocDirectory );
		final Elements blockLists = constantValuesJson.select( "ul.block-list" );
		for ( int bl = 0; bl < blockLists.size(); bl++ ) {
			final Element blockList = blockLists.get( bl );
			final String className = blockList.selectFirst( "span" ).text();

			final SettingsDocSection docSection = findMatchingDocSection( className, sections );
			if ( docSection == null ) {
				// does not match any defined sections, skip it
				continue;
			}

			final SortedSet<SettingDescriptor> docSectionSettings = findSettingDescriptors( docSection, result );
			final Map<String,Element> classFieldJavadocs = extractClassFieldJavadocs( className, javadocDirectory );

			final Element tableDiv = blockList.selectFirst( ".summary-table" );
			final Elements constantFqnColumns = tableDiv.select( ".col-first" );
			final Elements constantValueColumns = tableDiv.select( ".col-last" );

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
				final String simpleFieldName = constantFqn.substring( constantFqn.lastIndexOf( '.' ) );
				final Element fieldJavadocElement = classFieldJavadocs.get( simpleFieldName );

				final SettingDescriptor settingDescriptor = new SettingDescriptor(
						stripQuotes( constantValue ),
						convertFieldJavadocHtmlToAsciidoc(
								fieldJavadocElement,
								className,
								simpleFieldName,
								publishedJavadocsUrl
						)
//						extractJavadoc(
//								settingsClassJavadocJson,
//								className,
//								withoutPackagePrefix( constantFqn ),
//								publishedJavadocsUrl
//						)
				);
				docSectionSettings.add( settingDescriptor );
			}
		}

		return result;
	}

	public static Document loadConstants(Directory javadocDirectory) {
		try {
			final File constantValuesFile = javadocDirectory.file( "constant-values.html" ).getAsFile();
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
			if ( entry.getValue().getSettingsClassName().equals( className ) ) {
				return entry.getValue();
			}
		}
		return null;
	}

	private static SortedSet<SettingDescriptor> findSettingDescriptors(
			SettingsDocSection docSection,
			SortedMap<SettingsDocSection, SortedSet<SettingDescriptor>> map) {
		final SortedSet<SettingDescriptor> existing = map.get( docSection );
		if ( existing != null ) {
			return existing;
		}

		final SortedSet<SettingDescriptor> created = new TreeSet<>( SettingDescriptor.BY_NAME );
		map.put( docSection, created );
		return created;
	}

	private static Map<String, Element> extractClassFieldJavadocs(
			String className,
			Directory javadocDirectory) {
		System.out.println( "Processing Javadoc for " + className );
		final Map<String, Element> result = new HashMap<>();

		final Document document = loadClassJavadoc( className, javadocDirectory );
		final Elements fieldDetailSections = document.select( "section.detail" );
		for ( Element fieldDetailSection : fieldDetailSections ) {
			final String fieldName = fieldDetailSection.id();
			final Element fieldJavadocDiv = fieldDetailSection.selectFirst( "div.block" );
			result.put( fieldName, fieldJavadocDiv );
		}

		return result;
	}

	private static Document loadClassJavadoc(String enclosingClass, Directory javadocDirectory) {
		final String classJavadocFileName = enclosingClass.replace( ".", File.separator ) + ".html";
		final RegularFile classJavadocFile = javadocDirectory.file( classJavadocFileName );

		try {
			return Jsoup.parse( classJavadocFile.getAsFile() );
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

	/**
	 * Convert the DOM representation of the field Javadoc to Asciidoc format
	 *
	 * @param fieldJavadocElement The {@code <section class="detail"/>} element for the setting field
	 * @param className The name of the settings class
	 * @param simpleFieldName The name of the field defining the setting (relative to {@code className})
	 * @param publishedJavadocsUrl The (versioned) URL to Javadocs on the doc server
	 */
	private static String convertFieldJavadocHtmlToAsciidoc(
			Element fieldJavadocElement,
			String className,
			String simpleFieldName,
			String publishedJavadocsUrl) {
		// todo : here you go Marko :)
		return null;
	}

	private static String extractJavadoc(
			Document javadoc,
			String className,
			String constant,
			String publishedJavadocsUrl) {
		org.jsoup.nodes.Element block = javadoc.selectFirst( "#" + constant + " + ul li.blockList" );
		if ( block != null ) {
			for ( org.jsoup.nodes.Element link : block.getElementsByTag( "a" ) ) {
				String href = link.attr( "href" );
				// only update links if they are not external:
				if ( !link.hasClass( "externalLink" ) ) {
					if ( href.startsWith( "#" ) ) {
						href = withoutPackagePrefix( className ) + ".html" + href;
					}
					String packagePath = packagePrefix( className ).replace( ".", File.separator );
					href = publishedJavadocsUrl + packagePath + "/" + href;
				}
				else if ( href.contains( "/build/parents/" ) && href.contains( "/apidocs" ) ) {
					// means a link was to a class from other module and javadoc plugin generated some external link
					// that won't work. So we replace it:
					href = publishedJavadocsUrl + href.substring( href.indexOf( "/apidocs" ) + "/apidocs".length() );
				}
				link.attr( "href", href );
			}

			Elements result = new Elements();
			for ( org.jsoup.nodes.Element child : block.children() ) {
				if ( "h4".equalsIgnoreCase( child.tagName() ) || "pre".equalsIgnoreCase( child.tagName() ) ) {
					continue;
				}
				result.add( child );
			}

			return convertToAsciidoc( result );
		}
		return "";
	}

	private static String convertToAsciidoc(Elements elements) {
		StringBuilder doc = new StringBuilder( "" );
		for ( Element element : elements ) {
			convertToAsciidoc( element, doc, false );
		}

		return doc.toString();
	}

	private static void convertToAsciidoc(Node node, StringBuilder doc, boolean innerBlock) {
		if ( node instanceof Element ) {
			Element element = (Element) node;
			String tag = element.tagName();
			if ( "p".equalsIgnoreCase( tag ) || "div".equalsIgnoreCase( tag ) || "dl".equalsIgnoreCase( tag ) ) {
				if ( doc.length() != 0 ) {
					if ( !innerBlock ) {
						doc.append( "\n+" );
					}
					doc.append( "\n\n" );
				}
				boolean deprecation = element.hasClass( "deprecationBlock" );
				if ( deprecation ) {
					doc.append( "+\n[WARNING]\n====\n" );
				}
				for ( Node child : element.childNodes() ) {
					convertToAsciidoc( child, doc, deprecation );
				}
				doc.append( '\n' );
				if ( deprecation ) {
					doc.append( "====\n" );
				}
			}
			else if ( "a".equalsIgnoreCase( tag ) ) {
				convertToAsciidoc( element, "link:" + element.attr( "href" ) + "[", "]", doc, innerBlock );
			}
			else if ( "code".equalsIgnoreCase( tag ) ) {
				convertToAsciidoc( element, "`", "`", doc, innerBlock );
			}
			else if ( "strong".equalsIgnoreCase( tag ) || "em".equalsIgnoreCase( tag ) || "b".equalsIgnoreCase( tag ) ) {
				convertToAsciidoc( element, "**", "**", doc, innerBlock );
			}
			else if ( "ul".equalsIgnoreCase( tag ) || "ol".equalsIgnoreCase( tag ) ) {
				if ( doc.lastIndexOf( "\n" ) != doc.length() - 1 ) {
					doc.append( '\n' );
				}
				convertToAsciidoc( element, "+\n", "", doc, innerBlock );
			}
			else if ( "li".equalsIgnoreCase( tag ) ) {
				convertToAsciidoc( element, "\n  * ", "", doc, innerBlock );
			}
			else if ( "dt".equalsIgnoreCase( tag ) ) {
				convertToAsciidoc( element, "+\n**", "**", doc, innerBlock );
			}
			else if ( "dd".equalsIgnoreCase( tag ) ) {
				convertToAsciidoc( element, " ", "", doc, innerBlock );
			}
			else if ( "span".equalsIgnoreCase( tag ) ) {
				if ( element.hasClass( "deprecatedLabel" ) ) {
					// label for deprecation, let's make it bold to stand out:
					convertToAsciidoc( element, "**", "**", doc, innerBlock );
				}
				else {
					// simply pass to render items:
					convertToAsciidoc( element, "", "", doc, innerBlock );
				}
			}
			else {
				// if we encounter an element that we are not handling - we want to fail as the result might be missing some details:
				throw new IllegalStateException( "Unknown element: " + element );
			}
		}
		else if ( node instanceof TextNode ) {
			if ( doc.lastIndexOf( "+\n\n" ) == doc.length() - "+\n\n".length() ) {
				// if it's a start of paragraph - remove any leading spaces:
				doc.append( ( (TextNode) node ).text().replaceAll( "^\\s+", "" ) );
			}
			else {
				doc.append( ( (TextNode) node ).text() );
			}
		}
		else {
			// if we encounter a node that we are not handling - we want to fail as the result might be missing some details:
			throw new IllegalStateException( "Unknown node: " + node );
		}
	}

	private static void convertToAsciidoc(
			Element element,
			String pre,
			String post,
			StringBuilder doc,
			boolean innerBlock) {
		doc.append( pre );
		for ( Node childNode : element.childNodes() ) {
			convertToAsciidoc( childNode, doc, innerBlock );
		}
		doc.append( post );
	}

	private static String withoutPackagePrefix(String className) {
		return className.substring( className.lastIndexOf( '.' ) + 1 );
	}

	private static String packagePrefix(String className) {
		return className.substring( 0, className.lastIndexOf( '.' ) );
	}
}
