/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import org.hibernate.processor.model.Metamodel;

import static org.hibernate.processor.util.StringUtil.nameToMethodName;

/**
 * @author Gavin King
 */
class TypedMetaAttribute extends NameMetaAttribute {
	private final String resultType;
	private final String referenceType;

	public TypedMetaAttribute(
			Metamodel annotationMetaEntity,
			String name,
			String prefix,
			String resultType,
			String referenceType) {
		super( annotationMetaEntity, name, prefix );
		this.resultType = resultType;
		this.referenceType = referenceType;
	}

	@Override
	public boolean hasTypedAttribute() {
		return true;
	}

	@Override
	public String getAttributeDeclarationString() {
		final Metamodel entity = getHostingEntity();
		return new StringBuilder()
				.append("\n/**\n * @see ")
				.append(entity.getQualifiedName())
				.append("\n **/\n")
				.append("public static volatile ")
				.append(entity.importType(referenceType))
				.append('<')
				.append(entity.importType(resultType))
				.append('>')
				.append(' ')
				.append('_')
				.append(nameToMethodName(getPropertyName()))
				.append(';')
				.toString();
	}
}
