/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import jakarta.annotation.Nullable;
import org.hibernate.processor.model.Metamodel;

import static org.hibernate.processor.util.Constants.STATEMENT_REFERENCE;
import static org.hibernate.processor.util.StringUtil.nameToMethodName;

/**
 * Represents a named mutation statement reference.
 *
 * @author Gavin King
 */
class StatementMetaAttribute extends NameMetaAttribute {
	private final String prefix;
	private final @Nullable String statement;

	public StatementMetaAttribute(
			Metamodel annotationMetaEntity,
			String name,
			String prefix,
			@Nullable String statement) {
		super( annotationMetaEntity, name, prefix );
		this.prefix = prefix;
		this.statement = statement;
	}

	@Override
	public boolean hasTypedAttribute() {
		return true;
	}

	@Override
	public String getAttributeNameDeclarationString() {
		final var declaration = new StringBuilder();
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
		final var entity = getHostingEntity();
		final var declaration = new StringBuilder();
		declaration
				.append("\n/**")
				.append("\n * The statement named {@value ")
				.append(prefix)
				.append(fieldName())
				.append("}\n");
		if ( statement != null ) {
			declaration.append(" * <pre>");
			statement.lines()
					.forEach( line -> declaration.append("\n * ").append( line ) );
			declaration.append("\n * </pre>\n");
		}
		declaration
				.append(" *\n * @see ")
				.append(entity.getQualifiedName())
				.append("\n **/\n")
				.append("public static volatile ")
				.append(entity.importType(STATEMENT_REFERENCE))
				.append(' ');
		appendFieldName( declaration );
		declaration.append(';');
		return declaration.toString();
	}

	private void appendFieldName(StringBuilder declaration) {
		declaration
				.append('_')
				.append(nameToMethodName(getPropertyName()))
				.append('_');
	}
}
