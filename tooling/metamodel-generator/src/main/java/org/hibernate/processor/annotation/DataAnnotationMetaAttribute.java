/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.processor.model.MetaAttribute;
import org.hibernate.processor.model.Metamodel;

import javax.lang.model.element.Element;

import static org.hibernate.processor.util.Constants.JD_BOOLEAN_ATTRIBUTE;
import static org.hibernate.processor.util.Constants.JD_SORTABLE_ATTRIBUTE;
import static org.hibernate.processor.util.Constants.JD_TEXT_ATTRIBUTE;
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
	private final String metaType;
	private final String typeDeclaration;
	private final @Nullable String classLiteralType;
	private final @Nullable String path;

	public DataAnnotationMetaAttribute(
			AnnotationMetaEntity parent,
			Element element,
			String metaType,
			String typeDeclaration,
			@Nullable String classLiteralType,
			@Nullable String path) {
		this.element = element;
		this.parent = parent;
		this.metaType = metaType;
		this.typeDeclaration = typeDeclaration;
		this.classLiteralType = classLiteralType;
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

	@Override
	public String getAttributeDeclarationString() {
		final String className = parent.importType( parent.getQualifiedName() );
		final String elementName = element.getSimpleName().toString();
		final String memberName = path == null ? elementName : path + '.' + elementName;
		final String importedMetaType = parent.importType( metaType );
		return new StringBuilder()
				.append("\n/**\n * Static metamodel for attribute {@link ")
				.append(className)
				.append("#")
				.append(memberName)
				.append( "}\n **/\n" )
				.append( importedMetaType )
				.append( typeArguments( className ) )
				.append( ' ' )
				.append( getPropertyName().replace('.','_') )
				.append(" = ")
				.append( importedMetaType )
				.append( ".of(" )
				.append( className )
				.append( ".class, " )
				.append( fieldName() )
				.append( classLiteralArgument() )
				.append( ");" )
				.toString();
	}

	private String typeArguments(String className) {
		return switch ( metaType ) {
			case
				JD_TEXT_ATTRIBUTE,
				JD_BOOLEAN_ATTRIBUTE,
				JD_SORTABLE_ATTRIBUTE
					-> "<" + className + ">";
			default
					-> "<" + className + ", " + parent.importType( typeDeclaration ) + ">";
		};
	}

	private String classLiteralArgument() {
		return classLiteralType == null
				? ""
				: ", " + parent.importType( classLiteralType ) + ".class";
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
		return metaType;
	}

	@Override
	public String getTypeDeclaration() {
		return typeDeclaration;
	}

	@Override
	public String toString() {
		return new StringBuilder()
				.append( "DataAnnotationMetaAttribute" )
				.append( "{element=" ).append( element )
				.append( ", metaType='" ).append( metaType ).append( '\'' )
				.append( ", typeDeclaration='" ).append( typeDeclaration ).append( '\'' )
				.append( ", classLiteralType='" ).append( classLiteralType ).append( '\'' )
				.append( '}' ).toString();
	}
}
