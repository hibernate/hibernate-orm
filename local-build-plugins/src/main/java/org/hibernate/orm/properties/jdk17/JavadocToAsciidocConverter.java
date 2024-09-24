/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.properties.jdk17;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.orm.properties.Utils;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import static org.hibernate.orm.properties.Utils.packagePrefix;
import static org.hibernate.orm.properties.Utils.withoutPackagePrefix;

/**
 * @author Steve Ebersole
 */
public class JavadocToAsciidocConverter {

	/**
	 * Convert the DOM representation of the field Javadoc to Asciidoc format
	 *
	 * @param fieldJavadocElement The {@code <section class="detail"/>} element for the setting field
	 * @param className The name of the settings class
	 * @param simpleFieldName The name of the field defining the setting (relative to {@code className})
	 * @param publishedJavadocsUrl The (versioned) URL to Javadocs on the doc server
	 */
	public static String convertFieldJavadocHtmlToAsciidoc(
			Element fieldJavadocElement,
			String className,
			String simpleFieldName,
			String publishedJavadocsUrl) {
		Elements javadocsToConvert = cleanupFieldJavadocElement( fieldJavadocElement, className, simpleFieldName, publishedJavadocsUrl );

		return new DomToAsciidocConverter( javadocsToConvert ).convert();
	}

	private static Elements cleanupFieldJavadocElement(Element fieldJavadocElement,
													   String className,
													   String simpleFieldName,
													   String publishedJavadocsUrl) {
		// Before proceeding let's make sure that the javadoc structure is the one that we expect:
		if ( !isValidFieldJavadocStructure( fieldJavadocElement ) ) {
			throw new IllegalStateException( "Javadoc's DOM doesn't match the expected structure. " +
													 "This may lead to unexpected results in rendered configuration properties in the User Guide." );
		}

		fixUrls( className, publishedJavadocsUrl, fieldJavadocElement );

		// We want to take the javadoc block and the see links:
		Elements usefulDocsPart = new Elements();
		Element actualJavadocs = fieldJavadocElement.selectFirst( "div.block" );
		if ( actualJavadocs != null ) {
			usefulDocsPart.add( actualJavadocs );
		}
		usefulDocsPart.add( new Element( "div" )
									.appendChild( new Element( "b" ).text( "See: " ) )
									.appendChild( new Element( "a" )
														  .attr(
																  "href",
																  Utils.fieldJavadocLink( publishedJavadocsUrl, className, simpleFieldName )
														  ).text( Utils.withoutPackagePrefix( className ) + "." + simpleFieldName ) ) );

		return usefulDocsPart;
	}

	/**
	 * Any ORM links will be relative, and we want to prepend them with the {@code publishedJavadocsUrl}
	 * so that they are usable when the doc is published.
	 * Any external links should have a {@code externalLink} or {@code external-link} CSS class, we want to keep them unchanged.
	 * <p>
	 * NOTE: this method modifies the parsed DOM.
	 */
	private static void fixUrls(String className,
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
		//		<section class="detail" id="FIELD_NAME">
		//			<h3>FIELD_NAME</h3>
		//			<div class="member-signature"> we don't care about this div </div>
		//			<div class="block"> Actual Javadocs if present </div>
		//			<dl class="notes"> contains links to @see references </dl>
		//		</section>
		//
		if ( !"section".equalsIgnoreCase( fieldJavadocElement.tagName() ) ) {
			return false;
		}
		if ( !fieldJavadocElement.selectFirst( "h3" ).text().equals( fieldJavadocElement.id() ) ) {
			return false;
		}
		// since `div.block` can be absent if no javadocs were written we are trying to see if any of the divs is there;
		if ( fieldJavadocElement.selectFirst( "div.block" ) == null
				&& fieldJavadocElement.selectFirst( "dl.notes" ) == null
				&& fieldJavadocElement.selectFirst( "div.member-signature" ) == null
				&& !fieldJavadocElement.select( "div" ).isEmpty() ) {
			return false;
		}

		return true;
	}

	private static class DomToAsciidocConverter {
		private final Map<String, Consumer<Element>> elementVisitorsByTag = new HashMap<>();
		private final StringBuilder converted = new StringBuilder();
		private final Elements elements;

		public DomToAsciidocConverter(Elements elements) {
			elementVisitorsByTag.put( "a", this::visitLink );
			elementVisitorsByTag.put( "span", this::visitSpan );
			elementVisitorsByTag.put( "ul", this::visitUl );
			elementVisitorsByTag.put( "ol", this::visitUl );
			elementVisitorsByTag.put( "li", this::visitLi );
			elementVisitorsByTag.put( "b", this::visitBold );
			elementVisitorsByTag.put( "strong", this::visitBold );
			elementVisitorsByTag.put( "em", this::visitBold );
			elementVisitorsByTag.put( "code", this::visitCode );
			elementVisitorsByTag.put( "p", this::visitDiv );
			elementVisitorsByTag.put( "div", this::visitDiv );
			elementVisitorsByTag.put( "dl", this::visitDiv );

			this.elements = elements;
		}

		public String convert() {
			// Elements consist of nodes, that can themselves be elements or simple nodes.
			// Simple nodes are the ones that will produce text.
			// Elements are various tags that will be converted to some asciidoc syntax
			for ( Element element : elements ) {
				visitElement( element );
			}
			return converted.toString();
		}

		private void visitNode(Node node) {
			// We know about 2 types of nodes that we care about:
			// 	1. Element -- means that it is some tag,
			// 	so we will defer the decision what to do about it to the visitElement.
			//	2. TextNode -- simple text that we'll just add to the converted result.
			//
			// If we encounter something else -- let's fail,
			// so we can investigate what new element type it is and decide what to do about it.
			if ( node instanceof Element ) {
				visitElement( ( (Element) node ) );
			}
			else if ( node instanceof TextNode ) {
				visitTextNode( (TextNode) node );
			}
			else {
				visitUnknownNode( node );
			}
		}

		private void visitElement(Element element) {
			String tag = element.tagName();

			Consumer<Element> visitor = elementVisitorsByTag.get( tag );
			if ( visitor == null ) {
				// If we encounter an element that we are not handling --
				// we want to fail as the result might be missing some details:
				throw new IllegalStateException( "Unknown element: " + element );
			}

			visitor.accept( element );
		}

		private void visitDiv(Element div) {
			boolean deprecation = div.hasClass( "deprecationBlock" );
			if ( deprecation ) {
				converted.append( "[WARNING]\n====\n" );
			}
			for ( Node childNode : div.childNodes() ) {
				visitNode( childNode );
			}
			converted.append( '\n' );
			if ( deprecation ) {
				converted.append( "====\n" );
			}
		}

		private void visitCode(Element code) {
			converted.append( "`" );
			for ( Node childNode : code.childNodes() ) {
				visitNode( childNode );
			}
			converted.append( "`" );
		}

		private void visitBold(Element bold) {
			converted.append( "**" );
			for ( Node childNode : bold.childNodes() ) {
				visitNode( childNode );
			}
			converted.append( "**" );
		}

		private void visitLi(Element li) {
			converted.append( "\n  * " );
			for ( Node childNode : li.childNodes() ) {
				visitNode( childNode );
			}
		}

		private void visitUl(Element ul) {
			if ( converted.lastIndexOf( "\n" ) != converted.length() - 1 ) {
				converted.append( '\n' );
			}
			for ( Node childNode : ul.childNodes() ) {
				visitNode( childNode );
			}
		}

		private void visitSpan(Element span) {
			// If it is a label for deprecation, let's make it bold to stand out:
			boolean deprecatedLabel = span.hasClass( "deprecatedLabel" );
			if ( deprecatedLabel ) {
				converted.append( "**" );
			}
			for ( Node childNode : span.childNodes() ) {
				visitNode( childNode );
			}
			if ( deprecatedLabel ) {
				converted.append( "**" );
			}
		}

		private void visitLink(Element link) {
			converted.append( "link:" )
					.append( link.attr( "href" ) )
					.append( '[' );
			for ( Node childNode : link.childNodes() ) {
				visitNode( childNode );
			}
			converted.append( ']' );
		}

		private void visitTextNode(TextNode node) {
			if ( converted.lastIndexOf( "+\n" ) == converted.length() - "+\n".length() ) {
				// if it's a start of a paragraph - remove any leading spaces:
				converted.append( ( node ).text().replaceAll( "^\\s+", "" ) );
			}
			else {
				converted.append( ( node ).text() );
			}
		}

		private void visitUnknownNode(Node node) {
			// if we encounter a node that we are not handling - we want to fail as the result might be missing some details:
			throw new IllegalStateException( "Unknown node: " + node );
		}
	}
}
