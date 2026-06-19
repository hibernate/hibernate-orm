/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.model;

import org.hibernate.annotations.Array;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.boot.models.AttributeNature;
import org.hibernate.boot.mapping.internal.sources.ColumnSource;
import org.hibernate.boot.mapping.internal.sources.ComponentSource;
import org.hibernate.boot.mapping.internal.context.BindingContext;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.MemberDetails;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;

import static org.hibernate.boot.models.internal.DialectOverrideAnnotationHelper.getOverridableAnnotation;

/// Source-level intent for a basic-valued attribute or component member.
///
/// The intent describes the selectable and conversion facts requested by the
/// source model.  It deliberately stays at the source-fact level; physical
/// selectable/value objects remain materialization outputs.
///
/// @since 9.0
/// @author Steve Ebersole
public record BasicValueIntent(
		String formulaExpression,
		ColumnSource columnSource,
		String tableName,
		boolean insertable,
		boolean updatable,
		Integer arrayLength,
		String columnTransformerName,
		String customReadExpression,
		String customWriteExpression,
		Convert conversion) implements ValueIntent {
	@Override
	public AttributeNature nature() {
		return AttributeNature.BASIC;
	}

	public boolean isFormula() {
		return formulaExpression != null;
	}

	public static BasicValueIntent fromAttribute(
			MemberDetails member,
			BindingState bindingState,
			BindingContext bindingContext) {
		final var formulaAnn = formula( member, bindingState, bindingContext );
		if ( formulaAnn != null ) {
			return formulaIntent( formulaAnn.value(), directConversion( member ) );
		}

		final Column columnAnn = member.getDirectAnnotationUsage( Column.class );
		return columnIntent(
				ColumnSource.from( columnAnn ),
				columnAnn == null ? null : columnAnn.table(),
				columnAnn == null || columnAnn.insertable(),
				columnAnn == null || columnAnn.updatable(),
				member,
				directConversion( member )
		);
	}

	public static BasicValueIntent fromComponentMember(
			ComponentSource source,
			ComponentSource.ComponentMember member,
			BindingState bindingState,
			BindingContext bindingContext) {
		final var formulaAnn = formula( member.member(), bindingState, bindingContext );
		if ( formulaAnn != null ) {
			return formulaIntent( formulaAnn.value(), source.conversion( member.path(), member.member() ) );
		}

		return columnIntent(
				source.columnSource( member.path(), member.member() ),
				null,
				true,
				true,
				member.member(),
				source.conversion( member.path(), member.member() )
		);
	}

	private static BasicValueIntent formulaIntent(String formula, Convert conversion) {
		return new BasicValueIntent(
				formula,
				null,
				null,
				true,
				true,
				null,
				null,
				null,
				null,
				conversion
		);
	}

	private static BasicValueIntent columnIntent(
			ColumnSource columnSource,
			String tableName,
			boolean insertable,
			boolean updatable,
			MemberDetails member,
			Convert conversion) {
		final Array arrayAnn = member.getDirectAnnotationUsage( Array.class );
		final ColumnTransformer transformerAnn = member.getDirectAnnotationUsage( ColumnTransformer.class );
		return new BasicValueIntent(
				null,
				columnSource,
				tableName,
				insertable,
				updatable,
				arrayAnn == null ? null : arrayAnn.length(),
				transformerAnn == null ? null : transformerAnn.forColumn(),
				transformerAnn == null ? null : transformerAnn.read(),
				transformerAnn == null ? null : transformerAnn.write(),
				conversion
		);
	}

	private static org.hibernate.annotations.Formula formula(
			MemberDetails member,
			BindingState bindingState,
			BindingContext bindingContext) {
		return getOverridableAnnotation(
				member,
				org.hibernate.annotations.Formula.class,
				bindingState.getDatabase().getDialect(),
				bindingContext.getBootstrapContext().getModelsContext()
		);
	}

	private static Convert directConversion(MemberDetails member) {
		final Convert directConversion = member.getDirectAnnotationUsage( Convert.class );
		return directConversion != null && StringHelper.isEmpty( directConversion.attributeName() )
				? directConversion
				: null;
	}
}
