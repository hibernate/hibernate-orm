/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dom4j.Attribute;
import org.dom4j.Branch;
import org.dom4j.CDATA;
import org.dom4j.Comment;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Entity;
import org.dom4j.InvalidXPathException;
import org.dom4j.Namespace;
import org.dom4j.Node;
import org.dom4j.ProcessingInstruction;
import org.dom4j.QName;
import org.dom4j.Text;
import org.dom4j.Visitor;
import org.dom4j.XPath;

/**
 * Wraps dom4j elements, allowing them to exist in a 
 * non-hierarchical structure.
 *
 * @author Gavin King
 */
public class ElementWrapper implements Element, Serializable {

	private Element element;
	private Element parent;
	
	public Element getElement() {
		return element;
	}

	public ElementWrapper(Element element) {
		this.element = element;
	}

	public QName getQName() {
		return element.getQName();
	}

	public QName getQName(String s) {
		return element.getQName( s );
	}

	public void setQName(QName qName) {
		element.setQName( qName );
	}

	public Namespace getNamespace() {
		return element.getNamespace();
	}

	public Namespace getNamespaceForPrefix(String s) {
		return element.getNamespaceForPrefix( s );
	}

	public Namespace getNamespaceForURI(String s) {
		return element.getNamespaceForURI( s );
	}

	public List getNamespacesForURI(String s) {
		return element.getNamespacesForURI( s );
	}

	public String getNamespacePrefix() {
		return element.getNamespacePrefix();
	}

	public String getNamespaceURI() {
		return element.getNamespaceURI();
	}

	public String getQualifiedName() {
		return element.getQualifiedName();
	}

	public List additionalNamespaces() {
		return element.additionalNamespaces();
	}

	public List declaredNamespaces() {
		return element.declaredNamespaces();
	}

	public Element addAttribute(String attrName, String text) {
		return element.addAttribute( attrName, text );
	}

	public Element addAttribute(QName attrName, String text) {
		return element.addAttribute( attrName, text );
	}

	public Element addComment(String text) {
		return element.addComment( text );
	}

	public Element addCDATA(String text) {
		return element.addCDATA( text );
	}

	public Element addEntity(String name, String text) {
		return element.addEntity( name, text );
	}

	public Element addNamespace(String prefix, String uri) {
		return element.addNamespace( prefix, uri );
	}

	public Element addProcessingInstruction(String target, String text) {
		return element.addProcessingInstruction( target, text );
	}

	public Element addProcessingInstruction(String target, Map data) {
		return element.addProcessingInstruction( target, data );
	}

	public Element addText(String text) {
		return element.addText( text );
	}

	public void add(Attribute attribute) {
		element.add( attribute );
	}

	public void add(CDATA cdata) {
		element.add( cdata );
	}

	public void add(Entity entity) {
		element.add( entity );
	}

	public void add(Text text) {
		element.add( text );
	}

	public void add(Namespace namespace) {
		element.add( namespace );
	}

	public boolean remove(Attribute attribute) {
		return element.remove( attribute );
	}

	public boolean remove(CDATA cdata) {
		return element.remove( cdata );
	}

	public boolean remove(Entity entity) {
		return element.remove( entity );
	}

	public boolean remove(Namespace namespace) {
		return element.remove( namespace );
	}

	public boolean remove(Text text) {
		return element.remove( text );
	}

	public boolean supportsParent() {
		return element.supportsParent();
	}

	public Element getParent() {
		return parent==null ? element.getParent() : parent;
	}

	public void setParent(Element parent) {
		element.setParent( parent );
		this.parent = parent;
	}

	public Document getDocument() {
		return element.getDocument();
	}

	public void setDocument(Document document) {
		element.setDocument( document );
	}

	public boolean isReadOnly() {
		return element.isReadOnly();
	}

	public boolean hasContent() {
		return element.hasContent();
	}

	public String getName() {
		return element.getName();
	}

	public void setName(String name) {
		element.setName( name );
	}

	public String getText() {
		return element.getText();
	}

	public void setText(String text) {
		element.setText( text );
	}

	public String getTextTrim() {
		return element.getTextTrim();
	}

	public String getStringValue() {
		return element.getStringValue();
	}

	public String getPath() {
		return element.getPath();
	}

	public String getPath(Element element) {
		return element.getPath( element );
	}

	public String getUniquePath() {
		return element.getUniquePath();
	}

	public String getUniquePath(Element element) {
		return element.getUniquePath( element );
	}

	public String asXML() {
		return element.asXML();
	}

	public void write(Writer writer) throws IOException {
		element.write( writer );
	}

	public short getNodeType() {
		return element.getNodeType();
	}

	public String getNodeTypeName() {
		return element.getNodeTypeName();
	}

	public Node detach() {
		if (parent!=null) {
			parent.remove(this);
			parent = null;
		}
		return element.detach();
	}

	public List selectNodes(String xpath) {
		return element.selectNodes( xpath );
	}

	public Object selectObject(String xpath) {
		return element.selectObject( xpath );
	}

	public List selectNodes(String xpath, String comparison) {
		return element.selectNodes( xpath, comparison );
	}

	public List selectNodes(String xpath, String comparison, boolean removeDups) {
		return element.selectNodes( xpath, comparison, removeDups );
	}

	public Node selectSingleNode(String xpath) {
		return element.selectSingleNode( xpath );
	}

	public String valueOf(String xpath) {
		return element.valueOf( xpath );
	}

	public Number numberValueOf(String xpath) {
		return element.numberValueOf( xpath );
	}

	public boolean matches(String xpath) {
		return element.matches( xpath );
	}

	public XPath createXPath(String xpath) throws InvalidXPathException {
		return element.createXPath( xpath );
	}

	public Node asXPathResult(Element element) {
		return element.asXPathResult( element );
	}

	public void accept(Visitor visitor) {
		element.accept( visitor );
	}

	public Object clone() {
		return element.clone();
	}

	public Object getData() {
		return element.getData();
	}

	public void setData(Object data) {
		element.setData( data );
	}

	public List attributes() {
		return element.attributes();
	}

	public void setAttributes(List list) {
		element.setAttributes( list );
	}

	public int attributeCount() {
		return element.attributeCount();
	}

	public Iterator attributeIterator() {
		return element.attributeIterator();
	}

	public Attribute attribute(int i) {
		return element.attribute( i );
	}

	public Attribute attribute(String name) {
		return element.attribute( name );
	}

	public Attribute attribute(QName qName) {
		return element.attribute( qName );
	}

	public String attributeValue(String name) {
		return element.attributeValue( name );
	}

	public String attributeValue(String name, String defaultValue) {
		return element.attributeValue( name, defaultValue );
	}

	public String attributeValue(QName qName) {
		return element.attributeValue( qName );
	}

	public String attributeValue(QName qName, String defaultValue) {
		return element.attributeValue( qName, defaultValue );
	}

	public void setAttributeValue(String name, String value) {
		element.setAttributeValue( name, value );
	}

	public void setAttributeValue(QName qName, String value) {
		element.setAttributeValue( qName, value );
	}

	public Element element(String name) {
		return element.element( name );
	}

	public Element element(QName qName) {
		return element.element( qName );
	}

	public List elements() {
		return element.elements();
	}

	public List elements(String name) {
		return element.elements( name );
	}

	public List elements(QName qName) {
		return element.elements( qName );
	}

	public Iterator elementIterator() {
		return element.elementIterator();
	}

	public Iterator elementIterator(String name) {
		return element.elementIterator( name );

	}

	public Iterator elementIterator(QName qName) {
		return element.elementIterator( qName );
	}

	public boolean isRootElement() {
		return element.isRootElement();
	}

	public boolean hasMixedContent() {
		return element.hasMixedContent();
	}

	public boolean isTextOnly() {
		return element.isTextOnly();
	}

	public void appendAttributes(Element element) {
		element.appendAttributes( element );
	}

	public Element createCopy() {
		return element.createCopy();
	}

	public Element createCopy(String name) {
		return element.createCopy( name );
	}

	public Element createCopy(QName qName) {
		return element.createCopy( qName );
	}

	public String elementText(String name) {
		return element.elementText( name );
	}

	public String elementText(QName qName) {
		return element.elementText( qName );
	}

	public String elementTextTrim(String name) {
		return element.elementTextTrim( name );
	}

	public String elementTextTrim(QName qName) {
		return element.elementTextTrim( qName );
	}

	public Node getXPathResult(int i) {
		return element.getXPathResult( i );
	}

	public Node node(int i) {
		return element.node( i );
	}

	public int indexOf(Node node) {
		return element.indexOf( node );
	}

	public int nodeCount() {
		return element.nodeCount();
	}

	public Element elementByID(String id) {
		return element.elementByID( id );
	}

	public List content() {
		return element.content();
	}

	public Iterator nodeIterator() {
		return element.nodeIterator();
	}

	public void setContent(List list) {
		element.setContent( list );
	}

	public void appendContent(Branch branch) {
		element.appendContent( branch );
	}

	public void clearContent() {
		element.clearContent();
	}

	public List processingInstructions() {
		return element.processingInstructions();
	}

	public List processingInstructions(String name) {
		return element.processingInstructions( name );
	}

	public ProcessingInstruction processingInstruction(String name) {
		return element.processingInstruction( name );
	}

	public void setProcessingInstructions(List list) {
		element.setProcessingInstructions( list );
	}

	public Element addElement(String name) {
		return element.addElement( name );
	}

	public Element addElement(QName qName) {
		return element.addElement( qName );
	}

	public Element addElement(String name, String text) {
		return element.addElement( name, text );

	}

	public boolean removeProcessingInstruction(String name) {
		return element.removeProcessingInstruction( name );
	}

	public void add(Node node) {
		element.add( node );
	}

	public void add(Comment comment) {
		element.add( comment );
	}

	public void add(Element element) {
		element.add( element );
	}

	public void add(ProcessingInstruction processingInstruction) {
		element.add( processingInstruction );
	}

	public boolean remove(Node node) {
		return element.remove( node );
	}

	public boolean remove(Comment comment) {
		return element.remove( comment );
	}

	public boolean remove(Element element) {
		return element.remove( element );
	}

	public boolean remove(ProcessingInstruction processingInstruction) {
		return element.remove( processingInstruction );
	}

	public void normalize() {
		element.normalize();
	}
	
	public boolean equals(Object other) {
		return element.equals(other);
	}
	
	public int hashCode() {
		return element.hashCode();
	}
	
	public String toString() {
		return element.toString();
	}

}
