/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.properties;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import org.gradle.api.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

public class ConfigurationPropertyCollector {

	private final ConfigPropertyHolder propertyHolder;
	private final Logger logger;

	// configs:
	private final Path javadocsLocation;
	private final String javadocsBaseLink;
	private final String anchor;
	private final String moduleName;

	public ConfigurationPropertyCollector(ConfigPropertyHolder propertyHolder, Logger logger, Path javadocsLocation,
			String javadocsBaseLink, String anchor, String moduleName) {
		this.propertyHolder = propertyHolder;
		this.logger = logger;
		this.javadocsLocation = javadocsLocation;
		this.javadocsBaseLink = javadocsBaseLink;
		this.anchor = anchor;
		this.moduleName = moduleName;

	}

	public void processClasses() {
		processClasses( locateConstants() );
	}

	private void processClasses(Document constants) {
		for ( Element table : constants.select( "table.constantsSummary" ) ) {
			String className = table.selectFirst( "caption" ).text();
			if ( className.endsWith( "Settings" ) && !className.contains( ".impl." ) && !className.contains( ".internal." ) ) {
				// assume that such class is a config class and we want to collect properties from it.
				Optional<Document> javadoc = obtainJavadoc( className );
				javadoc.ifPresent( doc -> {
					// go through constants:
					for ( Element row : table.select( "tr" ) ) {
						if ( row.hasClass( "altColor" ) || row.hasClass( "rowColor" ) ) {
							propertyHolder.add(
									new ConfigurationProperty()
											.key( stripQuotes( row.selectFirst( ".colLast" ).text() ) )
											.javadoc(
													extractJavadoc(
															doc,
															className,
															withoutPackagePrefix( row.selectFirst( ".colFirst a" ).id() )
													)
											)
											.sourceClass( className )
											.anchorPrefix( anchor )
											.moduleName( moduleName )
							);
						}
					}
				} );
			}
		}
	}

	private String stripQuotes(String value) {
		if ( value.startsWith( "\"" ) && value.endsWith( "\"" ) ) {
			return value.substring( 1, value.length() - 1 );
		}
		return value;
	}

	private String extractJavadoc(Document javadoc, String className, String constant) {
		org.jsoup.nodes.Element block = javadoc.selectFirst( "#" + constant + " + ul li.blockList" );
		if ( block != null ) {
			for ( org.jsoup.nodes.Element link : block.getElementsByTag( "a" ) ) {
				String href = link.attr( "href" );
				// only update links if they are not external:
				if ( !link.hasClass( "external-link" ) ) {
					if ( href.startsWith( "#" ) ) {
						href = withoutPackagePrefix( className ) + ".html" + href;
					}
					String packagePath = packagePrefix(className).replace( ".", File.separator );
					href = javadocsBaseLink + packagePath + "/" + href;
				}
				else if ( href.contains( "/build/parents/" ) && href.contains( "/apidocs" ) ) {
					// means a link was to a class from other module and javadoc plugin generated some external link
					// that won't work. So we replace it:
					href = javadocsBaseLink + href.substring( href.indexOf( "/apidocs" ) + "/apidocs".length() );
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

	private String convertToAsciidoc(Elements elements) {
		StringBuilder doc = new StringBuilder( "" );
		for ( Element element : elements ) {
			convertToAsciidoc( element, doc, false );
		}

		return doc.toString();
	}

	private void convertToAsciidoc(Node node, StringBuilder doc, boolean innerBlock) {
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

	private void convertToAsciidoc(Element element, String pre, String post, StringBuilder doc, boolean innerBlock) {
		doc.append( pre );
		for ( Node childNode : element.childNodes() ) {
			convertToAsciidoc( childNode, doc, innerBlock );
		}
		doc.append( post );
	}

	private String withoutPackagePrefix(String className) {
		return className.substring( className.lastIndexOf( '.' ) + 1 );
	}

	private String packagePrefix(String className) {
		return className.substring( 0, className.lastIndexOf( '.' ) );
	}

	private Optional<Document> obtainJavadoc(String enclosingClass) {
		try {
			Path docs = javadocsLocation.resolve(
					enclosingClass.replace( ".", File.separator ) + ".html"
			);

			return Optional.of( Jsoup.parse( docs.toFile() ) );
		}
		catch (IOException e) {
			logger.error( "Unable to access javadocs for " + enclosingClass, e );
		}
		return Optional.empty();
	}

	private Document locateConstants() {
		try {
			Path docs = javadocsLocation.resolve( "constant-values.html" );

			return Jsoup.parse( docs.toFile() );
		}
		catch (IOException e) {
			logger.error( "Unable to access javadocs `constant-values.html`", e );
			throw new IllegalStateException( e );
		}
	}
}
