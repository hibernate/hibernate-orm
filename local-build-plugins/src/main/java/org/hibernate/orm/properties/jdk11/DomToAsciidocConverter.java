/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.properties.jdk11;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

/**
 * @author Steve Ebersole
 */
class DomToAsciidocConverter {
	public static String convert(Elements elements) {
		final DomToAsciidocConverter converter = new DomToAsciidocConverter();
		return converter.convertInternal( elements );
	}

	public static String convert(Element element) {
		final DomToAsciidocConverter converter = new DomToAsciidocConverter();
		return converter.convertInternal( element );
	}

	private final Map<String, Consumer<Element>> elementVisitorsByTag = new HashMap<>();
	private final StringBuilder converted = new StringBuilder();

	public DomToAsciidocConverter() {
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
		elementVisitorsByTag.put( "dd", this::visitDiv );
	}

	private String convertInternal(Elements elements) {
		for ( Element element : elements ) {
			visitElement( element );
		}
		return converted.toString();
	}

	private String convertInternal(Element element) {
		visitElement( element );
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
		for ( Node childNode : div.childNodes() ) {
			visitNode( childNode );
		}
		converted.append( '\n' );
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
