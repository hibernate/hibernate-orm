/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.ast.builder;

import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.ColumnValueParameter;
import org.hibernate.sql.model.ast.ColumnValueParameterList;
import org.hibernate.sql.model.ast.ColumnWriteFragment;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * Builder for {@link ColumnValueBinding} instances
 *
 * @author Steve Ebersole
 */
public class ColumnValueBindingBuilder {

	/**
	 * rexgex used to split the write-expressions into chunks of (1) quoted strings and (2) everything else.
	 */
	private static final String SPLIT_REGEX = "[^\\s\"']+|\"([^\"]*)\"|'([^']*)'";
	private static final Pattern SPLIT_PATTERN = Pattern.compile( SPLIT_REGEX );


	public static ColumnValueBinding createValueBinding(
			@Nullable String writeExpression,
			SelectableMapping selectableMapping,
			MutatingTableReference mutatingTableReference,
			ParameterUsage parameterUsage,
			Consumer<Object> parameterConsumer) {
		final ColumnReference columnReference = new ColumnReference(
				mutatingTableReference,
				selectableMapping.getSelectionExpression(),
				selectableMapping.getJdbcMapping()
		);
		final ColumnWriteFragment columnWriteFragment = buildWriteFragment(
				writeExpression,
				selectableMapping,
				mutatingTableReference,
				columnReference,
				parameterUsage,
				parameterConsumer
		);
		return new ColumnValueBinding( columnReference, columnWriteFragment ) ;
	}

	public static ColumnWriteFragment buildWriteFragment(
			@Nullable String writeExpression,
			SelectableMapping selectableMapping,
			MutatingTableReference mutatingTableReference,
			ColumnReference columnReference,
			ParameterUsage parameterUsage,
			Consumer<Object> parameterConsumer) {
		if ( writeExpression == null ) {
			return null;
		}

		if ( writeExpression.equals( "?" )
				|| ( writeExpression.contains( "?" ) && !writeExpression.contains( "'" ) ) ) {
			return buildParameterizedWriteFragment( writeExpression, selectableMapping, mutatingTableReference, columnReference, parameterUsage, parameterConsumer );
		}

		if ( !writeExpression.contains( "?" ) ) {
			return new ColumnWriteFragment( writeExpression, selectableMapping );
		}

		if ( containsParameter( writeExpression ) ) {
			return buildParameterizedWriteFragment( writeExpression, selectableMapping, mutatingTableReference, columnReference, parameterUsage, parameterConsumer );
		}

		return new ColumnWriteFragment( writeExpression, selectableMapping );
	}

	private static ColumnWriteFragment buildParameterizedWriteFragment(
			String writeExpression,
			SelectableMapping selectableMapping,
			MutatingTableReference mutatingTableReference,
			ColumnReference columnReference,
			ParameterUsage parameterUsage,
			Consumer<Object> parameterConsumer) {
		final JdbcType jdbcType = selectableMapping.getJdbcMapping().getJdbcType();
		final EmbeddableMappingType aggregateMappingType =
				jdbcType instanceof AggregateJdbcType aggregateJdbcType
						? aggregateJdbcType.getEmbeddableMappingType()
						: null;
		if ( aggregateMappingType != null && !aggregateMappingType.shouldBindAggregateMapping() ) {
			final ColumnValueParameterList parameters = new ColumnValueParameterList(
					mutatingTableReference,
					parameterUsage,
					aggregateMappingType.getJdbcTypeCount()
			);
			aggregateMappingType.forEachSelectable( parameters );
			parameterConsumer.accept( parameters );

			return new ColumnWriteFragment( writeExpression, parameters, selectableMapping );
		}
		else {
			final ColumnValueParameter parameter = new ColumnValueParameter( columnReference, parameterUsage );
			parameterConsumer.accept( parameter );
			return new ColumnWriteFragment( writeExpression, parameter, selectableMapping );
		}
	}

	private static boolean containsParameter(String writeExpression) {
		final Matcher matcher = SPLIT_PATTERN.matcher( writeExpression );
		while ( matcher.find() ) {
			final String group = matcher.group();
			if ( group.startsWith( "'" ) && group.endsWith( "'" ) ) {
				continue;
			}

			if ( group.contains( "?" ) ) {
				return true;
			}
		}

		return false;
	}
}
