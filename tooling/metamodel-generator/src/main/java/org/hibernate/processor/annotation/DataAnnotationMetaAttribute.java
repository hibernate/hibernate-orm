/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.annotation;

import org.hibernate.processor.model.MetaAttribute;
import org.hibernate.processor.model.Metamodel;

import javax.lang.model.element.Element;

import static org.hibernate.processor.util.StringUtil.getUpperUnderscoreCaseFromLowerCamelCase;
import static org.hibernate.processor.util.TypeUtils.propertyName;

/**
 * Captures all information about an annotated persistent attribute.
 *
 * @author Gavin King
 */
public class DataAnnotationMetaAttribute implements MetaAttribute {

	final Element element;
	final AnnotationMetaEntity parent;
	private final String type;

	public DataAnnotationMetaAttribute(AnnotationMetaEntity parent, Element element, String type) {
		this.element = element;
		this.parent = parent;
		this.type = type;
	}

	@Override
	public boolean hasTypedAttribute() {
		return true;
	}

	@Override
	public boolean hasStringAttribute() {
		return true;
	}

	private boolean isTextual() {
		return String.class.getName().equals(type);
	}

	@Override
	public String getAttributeDeclarationString() {
		final String className = parent.importType( parent.getQualifiedName() );
		final String memberName = element.getSimpleName().toString();
		final String impl = isTextual()
				? parent.importType("jakarta.data.metamodel.impl.TextAttributeRecord")
				: parent.importType("jakarta.data.metamodel.impl.SortableAttributeRecord");
		return new StringBuilder()
				.append("\n/**\n * @see ")
				.append(className)
				.append( "#")
				.append(memberName)
				.append( "\n **/\n" )
				.append( "public static final " )
				.append( parent.importType( getMetaType() ) )
				.append( "<" )
				.append( className )
				.append( "> " )
				.append( getPropertyName() )
				.append(" = new ")
				.append( impl )
				.append( "<>(\"" )
				.append(memberName)
				.append( "\");" )
				.toString();
	}

	@Override
	public String getAttributeNameDeclarationString(){
		return new StringBuilder()
				.append("public static final ")
				.append(parent.importType(String.class.getName()))
				.append(" ")
				.append(getUpperUnderscoreCaseFromLowerCamelCase(getPropertyName()))
				.append(" = ")
				.append("\"")
				.append(getPropertyName())
				.append("\"")
				.append(";")
				.toString();
	}

	@Override
	public String getPropertyName() {
		return propertyName( parent, element );
	}

	@Override
	public Metamodel getHostingEntity() {
		return parent;
	}

	@Override
	public String getMetaType() {
		return isTextual()
				? "jakarta.data.metamodel.TextAttribute"
				: "jakarta.data.metamodel.SortableAttribute";
	}

	@Override
	public String getTypeDeclaration() {
		return type;
	}

	@Override
	public String toString() {
		return new StringBuilder()
				.append( "DataAnnotationMetaAttribute" )
				.append( "{element=" ).append( element )
				.append( ", type='" ).append( type ).append( '\'' )
				.append( '}' ).toString();
	}
}
