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

			org.jsoup.nodes.Element result = new org.jsoup.nodes.Element( "div" );
			for ( org.jsoup.nodes.Element child : block.children() ) {
				if ( "h4".equalsIgnoreCase( child.tagName() ) || "pre".equalsIgnoreCase( child.tagName() ) ) {
					continue;
				}
				result.appendChild( child );
			}

			return result.toString();
		}
		return "";
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
