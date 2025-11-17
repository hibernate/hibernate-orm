/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import org.hibernate.processor.model.MetaAttribute;
import org.hibernate.processor.model.Metamodel;

import javax.lang.model.element.Element;

import static org.hibernate.processor.util.Constants.STRING;
import static org.hibernate.processor.util.StringUtil.getUpperUnderscoreCaseFromLowerCamelCase;
import static org.hibernate.processor.util.TypeUtils.propertyName;

/**
 * Captures all information about an annotated persistent attribute.
 *
 * @author Max Andersen
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
public abstract class AnnotationMetaAttribute implements MetaAttribute {

	final Element element;
	final AnnotationMetaEntity parent;
	private final String type;

	public AnnotationMetaAttribute(AnnotationMetaEntity parent, Element element, String type) {
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

	@Override
	public String getAttributeDeclarationString() {
		return new StringBuilder()
				.append("\n/**\n * Static metamodel for attribute {@link ")
				.append(parent.getQualifiedName())
				.append('#')
				.append(element.getSimpleName())
				.append("}\n **/\n")
				.append("public static volatile ")
				.append(parent.importType(getMetaType()))
				.append('<')
				.append(parent.importType(parent.getQualifiedName()))
				.append(", ")
				.append(parent.importType(getTypeDeclaration()))
				.append('>')
				.append(' ')
				.append(getPropertyName())
				.append(';')
				.toString();
	}

	@Override
	public String getAttributeNameDeclarationString(){
		return new StringBuilder()
				.append("\n/**\n * @see ")
				.append("#")
				.append(getPropertyName())
				.append( "\n **/\n" )
				.append("public static final ")
				.append(parent.importType(STRING))
				.append(' ')
				.append(getUpperUnderscoreCaseFromLowerCamelCase(getPropertyName()))
				.append(" = ")
				.append('"')
				.append(getPropertyName())
				.append('"')
				.append(';')
				.toString();
	}

	@Override
	public String getPropertyName() {
		return propertyName(element);
	}

	@Override
	public Metamodel getHostingEntity() {
		return parent;
	}

	@Override
	public abstract String getMetaType();

	@Override
	public String getTypeDeclaration() {
		return type;
	}

	@Override
	public String toString() {
		return new StringBuilder()
				.append( "AnnotationMetaAttribute" )
				.append( "{element=" ).append( element )
				.append( ", type='" ).append( type ).append( '\'' )
				.append( '}' ).toString();
	}
}
