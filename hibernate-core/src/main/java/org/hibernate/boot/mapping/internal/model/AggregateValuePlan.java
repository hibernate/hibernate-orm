/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.model;

import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.boot.mapping.internal.sources.ComponentSource;
import org.hibernate.type.SqlTypes;

/**
 * Resolved aggregate value facts consumed by the legacy aggregate bridge.
 * <p>
 * This plan is deliberately internal to the new boot pipeline.  It names the
 * aggregate column storage, the normalized aggregate value JDBC type, and the
 * logical aggregate member container before those facts are projected into
 * {@code Component}, {@code BasicValue}, {@code AggregateColumn}, and
 * aggregate component finalization.
 *
 * @since 9.0
 * @author Steve Ebersole
 */
public record AggregateValuePlan(
		AggregateMappingIntent intent,
		QualifiedName structName,
		String structNameText,
		Integer aggregateJdbcTypeCode,
		Integer aggregateValueJdbcTypeCode,
		Integer aggregateColumnSqlTypeCode,
		String aggregateColumnSqlType,
		boolean explicitAggregateJavaType,
		AggregateMemberContainer memberContainer) {
	public static AggregateValuePlan from(AggregateMappingIntent intent, BindingState state) {
		return from( intent, state, AggregateMemberContainer.from( intent ) );
	}

	public static AggregateValuePlan from(
			AggregateMappingIntent intent,
			BindingState state,
			AggregateMemberContainer memberContainer) {
		final QualifiedName structName = intent.structName( state );
		final String structNameText = structName == null ? null : structName.render();
		final Integer aggregateJdbcTypeCode = intent.jdbcTypeCode();
		return new AggregateValuePlan(
				intent,
				structName,
				structNameText,
				aggregateJdbcTypeCode,
				aggregateJdbcTypeCode == null ? null : aggregateValueJdbcTypeCode( aggregateJdbcTypeCode ),
				aggregateColumnSqlTypeCode( intent, structNameText, aggregateJdbcTypeCode, state ),
				aggregateColumnSqlType( intent, structNameText, state ),
				!intent.plural() || intent.source().kind() != ComponentSource.Kind.EMBEDDED_ATTRIBUTE,
				memberContainer
		);
	}

	private static Integer aggregateColumnSqlTypeCode(
			AggregateMappingIntent intent,
			String structNameText,
			Integer aggregateJdbcTypeCode,
			BindingState state) {
		if ( structNameText != null ) {
			return intent.plural() ? getStructPluralSqlTypeCode( state ) : SqlTypes.STRUCT;
		}
		return aggregateJdbcTypeCode;
	}

	private static String aggregateColumnSqlType(
			AggregateMappingIntent intent,
			String structNameText,
			BindingState state) {
		if ( structNameText == null ) {
			return null;
		}
		return intent.plural()
				? state.getDatabase()
						.getDialect()
						.getArrayTypeName( null, structNameText, null )
				: structNameText;
	}

	private static int getStructPluralSqlTypeCode(BindingState state) {
		return switch ( state.getMetadataBuildingContext().getBuildingPlan().getPreferredSqlTypeCodeForArray() ) {
			case SqlTypes.ARRAY -> SqlTypes.STRUCT_ARRAY;
			case SqlTypes.TABLE -> SqlTypes.STRUCT_TABLE;
			default -> throw new UnsupportedOperationException(
					"Dialect does not support structured array types: "
					+ state.getDatabase().getDialect().getClass().getName()
			);
		};
	}

	private static int aggregateValueJdbcTypeCode(int aggregateJdbcTypeCode) {
		return switch ( aggregateJdbcTypeCode ) {
			case SqlTypes.JSON_ARRAY -> SqlTypes.JSON;
			case SqlTypes.XML_ARRAY -> SqlTypes.SQLXML;
			default -> aggregateJdbcTypeCode;
		};
	}
}
