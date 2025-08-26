/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import org.jspecify.annotations.Nullable;
import org.hibernate.processor.model.MetaAttribute;
import org.hibernate.processor.model.Metamodel;

import javax.lang.model.element.Element;

import static org.hibernate.processor.util.Constants.STRING;
import static org.hibernate.processor.util.StringUtil.getUpperUnderscoreCaseFromLowerCamelCase;
import static org.hibernate.processor.util.TypeUtils.propertyName;

/**
 * Captures all information about an annotated persistent attribute.
 *
 * @author Gavin King
 */
public class DataAnnotationMetaAttribute implements MetaAttribute {

	private final Element element;
	private final AnnotationMetaEntity parent;
	private final String type;
	private final @Nullable String path;

	public DataAnnotationMetaAttribute(
			AnnotationMetaEntity parent, Element element, String type, @Nullable String path) {
		this.element = element;
		this.parent = parent;
		this.type = type;
		this.path = path;
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
		return STRING.equals(type);
	}

	@Override
	public String getAttributeDeclarationString() {
		final String className = parent.importType( parent.getQualifiedName() );
		final String elementName = element.getSimpleName().toString();
		final String memberName = path == null ? elementName : path + '.' + elementName;
		final String impl = isTextual()
				? parent.importType("jakarta.data.metamodel.impl.TextAttributeRecord")
				: parent.importType("jakarta.data.metamodel.impl.SortableAttributeRecord");
		return new StringBuilder()
				.append("\n/**\n * Static metamodel for attribute {@link ")
				.append(className)
				.append("#")
				.append(memberName)
				.append( "}\n **/\n" )
				.append( parent.importType( getMetaType() ) )
				.append( "<" )
				.append( className )
				.append( "> " )
				.append( getPropertyName().replace('.','_') )
				.append(" = new ")
				.append( impl )
				.append( "<>(" )
				.append( fieldName() )
				.append( ");" )
				.toString();
	}

	@Override
	public String getAttributeNameDeclarationString(){
		return new StringBuilder()
				.append("\n/**\n * @see ")
				.append("#")
				.append( getPropertyName().replace('.','_') )
				.append( "\n **/\n" )
				.append(parent.importType(STRING))
				.append(" ")
				.append(fieldName())
				.append(" = ")
				.append("\"")
				.append(getPropertyName())
				.append("\"")
				.append(";")
				.toString();
	}

	private String fieldName() {
		return getUpperUnderscoreCaseFromLowerCamelCase(getPropertyName().replace('.', '_'));
	}

	@Override
	public String getPropertyName() {
		final String propertyName = propertyName(element);
		return path == null ? propertyName : path + '.' + propertyName;
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
