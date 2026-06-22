/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.model;

import org.hibernate.annotations.Array;
import org.hibernate.annotations.CollectionId;
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
		return fromAttribute( member, null, bindingState, bindingContext );
	}

	public static BasicValueIntent fromAttribute(
			MemberDetails member,
			jakarta.persistence.AttributeOverride attributeOverride,
			BindingState bindingState,
			BindingContext bindingContext) {
		final var formulaAnn = formula( member, bindingState, bindingContext );
		if ( formulaAnn != null ) {
			return formulaIntent( formulaAnn.value(), directConversion( member ) );
		}

		final Column columnAnn = member.getDirectAnnotationUsage( Column.class );
		final Column effectiveColumn = attributeOverride == null ? columnAnn : attributeOverride.column();
		return columnIntent(
				ColumnSource.from( effectiveColumn ),
				effectiveColumn == null ? null : effectiveColumn.table(),
				effectiveColumn == null || effectiveColumn.insertable(),
				effectiveColumn == null || effectiveColumn.updatable(),
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

		final ColumnSource columnSource = source.columnSource( member.path(), member.member() );
		return columnIntent(
				columnSource,
				null,
				columnSource == null || columnSource.insertable( true ),
				columnSource == null || columnSource.updatable( true ),
				member.member(),
				source.conversion( member.path(), member.member() )
		);
	}

	public static BasicValueIntent fromCollectionElement(
			org.hibernate.boot.mapping.internal.sources.CollectionSource source) {
		return columnIntent(
				source.elementColumn() == null ? null : ColumnSource.from( source.elementColumn() ),
				null,
				true,
				true,
				source.member(),
				directConversion( source.member() )
		);
	}

	public static BasicValueIntent fromListIndex(
			org.hibernate.boot.mapping.internal.sources.CollectionSource source) {
		return columnIntent(
				ColumnSource.from( source.orderColumn() ),
				null,
				source.orderColumn() == null || source.orderColumn().insertable(),
				source.orderColumn() == null || source.orderColumn().updatable(),
				source.member(),
				null
		);
	}

	public static BasicValueIntent fromMapKey(
			org.hibernate.boot.mapping.internal.sources.CollectionSource source) {
		return columnIntent(
				ColumnSource.from( source.mapKeyColumn() ),
				null,
				true,
				true,
				source.member(),
				directConversion( source.member() )
		);
	}

	public static BasicValueIntent fromCollectionId(
			org.hibernate.boot.mapping.internal.sources.CollectionSource source) {
		final CollectionId collectionId = source.member().getDirectAnnotationUsage( CollectionId.class );
		return columnIntent(
				collectionId == null ? null : ColumnSource.from( collectionId.column() ),
				null,
				true,
				true,
				source.member(),
				null
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
