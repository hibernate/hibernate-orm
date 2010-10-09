/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.proxy.dom4j;

import org.dom4j.Element;
import org.dom4j.QName;
import org.dom4j.Namespace;
import org.dom4j.Attribute;
import org.dom4j.CDATA;
import org.dom4j.Entity;
import org.dom4j.Text;
import org.dom4j.Node;
import org.dom4j.Branch;
import org.dom4j.ProcessingInstruction;
import org.dom4j.Comment;
import org.dom4j.Document;
import org.dom4j.XPath;
import org.dom4j.InvalidXPathException;
import org.dom4j.Visitor;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

import java.io.Serializable;
import java.io.Writer;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Iterator;

/**
 * Proxy for "dom4j" entity representations.
 *
 * @author Steve Ebersole
 */
public class Dom4jProxy implements HibernateProxy, Element, Serializable {

	private Dom4jLazyInitializer li;

	public Dom4jProxy(Dom4jLazyInitializer li) {
		this.li = li;
	}

	public Object writeReplace() {
		return this;
	}

	public LazyInitializer getHibernateLazyInitializer() {
		return li;
	}

	public QName getQName() {
		return target().getQName();
	}

	public QName getQName(String s) {
		return target().getQName( s );
	}

	public void setQName(QName qName) {
		target().setQName( qName );
	}

	public Namespace getNamespace() {
		return target().getNamespace();
	}

	public Namespace getNamespaceForPrefix(String s) {
		return target().getNamespaceForPrefix( s );
	}

	public Namespace getNamespaceForURI(String s) {
		return target().getNamespaceForURI( s );
	}

	public List getNamespacesForURI(String s) {
		return target().getNamespacesForURI( s );
	}

	public String getNamespacePrefix() {
		return target().getNamespacePrefix();
	}

	public String getNamespaceURI() {
		return target().getNamespaceURI();
	}

	public String getQualifiedName() {
		return target().getQualifiedName();
	}

	public List additionalNamespaces() {
		return target().additionalNamespaces();
	}

	public List declaredNamespaces() {
		return target().declaredNamespaces();
	}

	public Element addAttribute(String attrName, String text) {
		return target().addAttribute( attrName, text );
	}

	public Element addAttribute(QName attrName, String text) {
		return target().addAttribute( attrName, text );
	}

	public Element addComment(String text) {
		return target().addComment( text );
	}

	public Element addCDATA(String text) {
		return target().addCDATA( text );
	}

	public Element addEntity(String name, String text) {
		return target().addEntity( name, text );
	}

	public Element addNamespace(String prefix, String uri) {
		return target().addNamespace( prefix, uri );
	}

	public Element addProcessingInstruction(String target, String text) {
		return target().addProcessingInstruction( target, text );
	}

	public Element addProcessingInstruction(String target, Map data) {
		return target().addProcessingInstruction( target, data );
	}

	public Element addText(String text) {
		return target().addText( text );
	}

	public void add(Attribute attribute) {
		target().add( attribute );
	}

	public void add(CDATA cdata) {
		target().add( cdata );
	}

	public void add(Entity entity) {
		target().add( entity );
	}

	public void add(Text text) {
		target().add( text );
	}

	public void add(Namespace namespace) {
		target().add( namespace );
	}

	public boolean remove(Attribute attribute) {
		return target().remove( attribute );
	}

	public boolean remove(CDATA cdata) {
		return target().remove( cdata );
	}

	public boolean remove(Entity entity) {
		return target().remove( entity );
	}

	public boolean remove(Namespace namespace) {
		return target().remove( namespace );
	}

	public boolean remove(Text text) {
		return target().remove( text );
	}

	public boolean supportsParent() {
		return target().supportsParent();
	}

	public Element getParent() {
		return target().getParent();
	}

	public void setParent(Element element) {
		target().setParent( element );
	}

	public Document getDocument() {
		return target().getDocument();
	}

	public void setDocument(Document document) {
		target().setDocument( document );
	}

	public boolean isReadOnly() {
		return target().isReadOnly();
	}

	public boolean hasContent() {
		return target().hasContent();
	}

	public String getName() {
		return target().getName();
	}

	public void setName(String name) {
		target().setName( name );
	}

	public String getText() {
		return target().getText();
	}

	public void setText(String text) {
		target().setText( text );
	}

	public String getTextTrim() {
		return target().getTextTrim();
	}

	public String getStringValue() {
		return target().getStringValue();
	}

	public String getPath() {
		return target().getPath();
	}

	public String getPath(Element element) {
		return target().getPath( element );
	}

	public String getUniquePath() {
		return target().getUniquePath();
	}

	public String getUniquePath(Element element) {
		return target().getUniquePath( element );
	}

	public String asXML() {
		return target().asXML();
	}

	public void write(Writer writer) throws IOException {
		target().write( writer );
	}

	public short getNodeType() {
		return target().getNodeType();
	}

	public String getNodeTypeName() {
		return target().getNodeTypeName();
	}

	public Node detach() {
		Element parent = target().getParent();
		if (parent!=null) parent.remove(this);
		return target().detach();
	}

	public List selectNodes(String xpath) {
		return target().selectNodes( xpath );
	}

	public Object selectObject(String xpath) {
		return target().selectObject( xpath );
	}

	public List selectNodes(String xpath, String comparison) {
		return target().selectNodes( xpath, comparison );
	}

	public List selectNodes(String xpath, String comparison, boolean removeDups) {
		return target().selectNodes( xpath, comparison, removeDups );
	}

	public Node selectSingleNode(String xpath) {
        return target().selectSingleNode( xpath );
	}

	public String valueOf(String xpath) {
		return target().valueOf( xpath );
	}

	public Number numberValueOf(String xpath) {
		return target().numberValueOf( xpath );
	}

	public boolean matches(String xpath) {
		return target().matches( xpath );
	}

	public XPath createXPath(String xpath) throws InvalidXPathException {
		return target().createXPath( xpath );
	}

	public Node asXPathResult(Element element) {
		return target().asXPathResult( element );
	}

	public void accept(Visitor visitor) {
		target().accept( visitor );
	}

	public Object clone() {
		return target().clone();
	}

	public Object getData() {
		return target().getData();
	}

	public void setData(Object data) {
		target().setData( data );
	}

	public List attributes() {
		return target().attributes();
	}

	public void setAttributes(List list) {
		target().setAttributes( list );
	}

	public int attributeCount() {
		return target().attributeCount();
	}

	public Iterator attributeIterator() {
		return target().attributeIterator();
	}

	public Attribute attribute(int i) {
		return target().attribute( i );
	}

	public Attribute attribute(String name) {
		return target().attribute( name );
	}

	public Attribute attribute(QName qName) {
		return target().attribute( qName );
	}

	public String attributeValue(String name) {
		return target().attributeValue( name );
	}

	public String attributeValue(String name, String defaultValue) {
		return target().attributeValue( name, defaultValue );
	}

	public String attributeValue(QName qName) {
		return target().attributeValue( qName );
	}

	public String attributeValue(QName qName, String defaultValue) {
		return target().attributeValue( qName, defaultValue );
	}

	/**
	 * @deprecated
	 */
	public void setAttributeValue(String name, String value) {
		target().setAttributeValue( name, value );
	}

	/**
	 * @deprecated
	 */
	public void setAttributeValue(QName qName, String value) {
		target().setAttributeValue( qName, value );
	}

	public Element element(String name) {
		return target().element( name );
	}

	public Element element(QName qName) {
		return target().element( qName );
	}

	public List elements() {
		return target().elements();
	}

	public List elements(String name) {
		return target().elements( name );
	}

	public List elements(QName qName) {
		return target().elements( qName );
	}

	public Iterator elementIterator() {
		return target().elementIterator();
	}

	public Iterator elementIterator(String name) {
		return target().elementIterator( name );

	}

	public Iterator elementIterator(QName qName) {
		return target().elementIterator( qName );
	}

	public boolean isRootElement() {
		return target().isRootElement();
	}

	public boolean hasMixedContent() {
		return target().hasMixedContent();
	}

	public boolean isTextOnly() {
		return target().isTextOnly();
	}

	public void appendAttributes(Element element) {
		target().appendAttributes( element );
	}

	public Element createCopy() {
		return target().createCopy();
	}

	public Element createCopy(String name) {
		return target().createCopy( name );
	}

	public Element createCopy(QName qName) {
		return target().createCopy( qName );
	}

	public String elementText(String name) {
		return target().elementText( name );
	}

	public String elementText(QName qName) {
		return target().elementText( qName );
	}

	public String elementTextTrim(String name) {
		return target().elementTextTrim( name );
	}

	public String elementTextTrim(QName qName) {
		return target().elementTextTrim( qName );
	}

	public Node getXPathResult(int i) {
		return target().getXPathResult( i );
	}

	public Node node(int i) {
		return target().node( i );
	}

	public int indexOf(Node node) {
		return target().indexOf( node );
	}

	public int nodeCount() {
		return target().nodeCount();
	}

	public Element elementByID(String id) {
		return target().elementByID( id );
	}

	public List content() {
		return target().content();
	}

	public Iterator nodeIterator() {
		return target().nodeIterator();
	}

	public void setContent(List list) {
		target().setContent( list );
	}

	public void appendContent(Branch branch) {
		target().appendContent( branch );
	}

	public void clearContent() {
		target().clearContent();
	}

	public List processingInstructions() {
		return target().processingInstructions();
	}

	public List processingInstructions(String name) {
		return target().processingInstructions( name );
	}

	public ProcessingInstruction processingInstruction(String name) {
		return target().processingInstruction( name );
	}

	public void setProcessingInstructions(List list) {
		target().setProcessingInstructions( list );
	}

	public Element addElement(String name) {
		return target().addElement( name );
	}

	public Element addElement(QName qName) {
		return target().addElement( qName );
	}

	public Element addElement(String name, String text) {
		return target().addElement( name, text );

	}

	public boolean removeProcessingInstruction(String name) {
		return target().removeProcessingInstruction( name );
	}

	public void add(Node node) {
		target().add( node );
	}

	public void add(Comment comment) {
		target().add( comment );
	}

	public void add(Element element) {
		target().add( element );
	}

	public void add(ProcessingInstruction processingInstruction) {
		target().add( processingInstruction );
	}

	public boolean remove(Node node) {
		return target().remove( node );
	}

	public boolean remove(Comment comment) {
		return target().remove( comment );
	}

	public boolean remove(Element element) {
		return target().remove( element );
	}

	public boolean remove(ProcessingInstruction processingInstruction) {
		return target().remove( processingInstruction );
	}

	public void normalize() {
		target().normalize();
	}

	private Element target() {
		return li.getElement();
	}
}
