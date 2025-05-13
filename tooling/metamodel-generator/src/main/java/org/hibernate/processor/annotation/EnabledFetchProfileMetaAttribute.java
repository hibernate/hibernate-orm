/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import org.hibernate.processor.model.Metamodel;

import static org.hibernate.processor.util.StringUtil.nameToMethodName;

/**
 * Represents a named fetch profile.
 *
 * @author Gavin King
 */
class EnabledFetchProfileMetaAttribute extends NameMetaAttribute {
	private final String prefix;
	private final String referenceType;

	public EnabledFetchProfileMetaAttribute(
			Metamodel annotationMetaEntity,
			String name,
			String prefix,
			String referenceType) {
		super( annotationMetaEntity, name, prefix );
		this.prefix = prefix;
		this.referenceType = referenceType;
	}

	@Override
	public boolean hasTypedAttribute() {
		return true;
	}

	@Override
	public String getAttributeNameDeclarationString() {
		StringBuilder declaration = new StringBuilder();
		declaration
				.append("\n/**\n * @see ")
				.append("#");
		appendFieldName( declaration );
		return declaration
				.append( "\n **/\n" )
				.append(super.getAttributeNameDeclarationString())
				.toString();
	}

	@Override
	public String getAttributeDeclarationString() {
		final Metamodel entity = getHostingEntity();
		final StringBuilder declaration = new StringBuilder();
		declaration
				.append("\n/**")
				.append("\n * The fetch profile named {@value ")
				.append(prefix)
				.append(fieldName())
				.append("}\n")
				.append(" *\n * @see ")
				.append(entity.getQualifiedName())
				.append("\n **/\n")
				.append("public static final ")
				.append(entity.importType(referenceType))
				.append(' ');
		appendFieldName( declaration );
		declaration
				.append(" = new ")
				.append(entity.importType(referenceType))
				.append("(")
				.append(prefix)
				.append(fieldName())
				.append(");");
		return declaration.toString();
	}

	private void appendFieldName(StringBuilder declaration) {
		declaration
				.append('_')
				.append(nameToMethodName(getPropertyName()));
	}
}
