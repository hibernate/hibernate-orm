/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import org.jspecify.annotations.Nullable;
import org.hibernate.processor.model.MetaAttribute;
import org.hibernate.processor.model.Metamodel;
import org.hibernate.processor.util.Constants;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;

import java.util.TreeSet;

import static org.hibernate.processor.util.Constants.NONNULL;
import static org.hibernate.processor.util.StringUtil.nameToFieldName;

/**
 * @author Gavin King
 */
class NamedQueryMethod implements MetaAttribute {
	private final AnnotationMeta annotationMeta;
	private final SqmSelectStatement<?> select;
	private final String name;
	private final boolean belongsToRepository;
	private final boolean reactive;
	private final String sessionVariableName;
	private final boolean addNonnullAnnotation;
	private final String resultClass;

	public NamedQueryMethod(
			AnnotationMeta annotationMeta,
			SqmSelectStatement<?> select,
			String name,
			boolean belongsToRepository,
			@Nullable String sessionType,
			String sessionVariableName,
			boolean addNonnullAnnotation,
			String resultClass) {
		this.annotationMeta = annotationMeta;
		this.select = select;
		this.name = name;
		this.belongsToRepository = belongsToRepository;
		this.reactive = Constants.MUTINY_SESSION.equals(sessionType);
		this.sessionVariableName = sessionVariableName;
		this.addNonnullAnnotation = addNonnullAnnotation;
		this.resultClass = resultClass;
	}

	@Override
	public boolean hasTypedAttribute() {
		return true;
	}

	@Override
	public boolean hasStringAttribute() {
		return false;
	}

	@Override
	public String getAttributeDeclarationString() {
		final TreeSet<SqmParameter<?>> sortedParameters =
				new TreeSet<>( select.getSqmParameters() );
		StringBuilder declaration = new StringBuilder();
		comment( declaration );
		modifiers( declaration );
		returnType( declaration );
		parameters( sortedParameters, declaration );
		declaration
				.append(" {")
				.append("\n\treturn ")
				.append(sessionVariableName)
				.append(".createNamedQuery(")
				.append(fieldName())
				.append(", ")
				.append( annotationMeta.importType( resultClass ) )
				.append( ".class)");
		for ( SqmParameter<?> param : sortedParameters ) {
			declaration
					.append("\n\t\t\t.setParameter(")
					.append(param.getName() == null ? param.getPosition() : '"' + param.getName() + '"')
					.append(", ")
					.append(param.getName() == null ? "parameter" + param.getPosition() : param.getName())
					.append(')');
		}
		declaration
				.append("\n\t\t\t.getResultList();\n}");
		return declaration.toString();
	}

	private String fieldName() {
		return "QUERY_" + nameToFieldName(name);
	}

	void notNull(StringBuilder declaration) {
		if ( addNonnullAnnotation ) {
			declaration
					.append('@')
					.append(annotationMeta.importType(NONNULL))
					.append(' ');
		}
	}

	private void comment(StringBuilder declaration) {
		declaration
				.append("\n/**\n * Execute named query {@value #")
				.append(fieldName())
				.append("} defined by annotation of {@link ")
				.append(annotationMeta.getSimpleName())
				.append("}.\n **/\n");
	}

	private void modifiers(StringBuilder declaration) {
		declaration
				.append(belongsToRepository ? "public " : "public static ");
	}

	private void returnType(StringBuilder declaration) {
		if ( reactive ) {
			declaration
					.append(annotationMeta.importType(Constants.UNI))
					.append('<');
		}
		declaration
				.append(annotationMeta.importType(Constants.LIST))
				.append('<')
				.append( annotationMeta.importType( resultClass ) )
				.append("> ")
				.append(name);
		if ( reactive ) {
			declaration
					.append('>');
		}
	}

	private void parameters(TreeSet<SqmParameter<?>> sortedParameters, StringBuilder declaration) {
		declaration
				.append('(');
		if ( !belongsToRepository) {
			notNull( declaration );
			declaration
					.append(annotationMeta.importType(Constants.ENTITY_MANAGER))
					.append(" ")
					.append(sessionVariableName);
		}
		int i = 0;
		for ( SqmParameter<?> param : sortedParameters) {
			if ( 0 < i++ || !belongsToRepository) {
				declaration
						.append(", ");
			}
			if ( param.allowMultiValuedBinding() ) {
				declaration
						.append(annotationMeta.importType(Constants.LIST))
						.append('<')
						.append( parameterType( param ) )
						.append("> ")
						.append( parameterName( param ) );
			}
			else {
				declaration
						.append( parameterType( param ) )
						.append( " " )
						.append( parameterName( param ) );
			}
		}
		declaration
				.append(')');
	}

	private static String parameterName(SqmParameter<?> param) {
		return param.getName() == null ? "parameter" + param.getPosition() : param.getName();
	}

	private String parameterType(SqmParameter<?> param) {
		final SqmExpressible<?> expressible = param.getExpressible();
		final String paramType = expressible == null ? "unknown" : expressible.getTypeName(); //getTypeName() can return "unknown"
		return "unknown".equals(paramType) ? "Object" : annotationMeta.importType(paramType);
	}

	@Override
	public String getAttributeNameDeclarationString() {
		throw new UnsupportedOperationException("operation not supported");
	}

	@Override
	public String getMetaType() {
		throw new UnsupportedOperationException("operation not supported");
	}

	@Override
	public String getPropertyName() {
		return name;
	}

	@Override
	public String getTypeDeclaration() {
		return Constants.LIST;
	}

	@Override
	public Metamodel getHostingEntity() {
		return annotationMeta;
	}
}
