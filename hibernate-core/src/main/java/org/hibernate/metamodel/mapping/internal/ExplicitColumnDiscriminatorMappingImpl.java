/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.mapping.DiscriminatorConverter;
import org.hibernate.metamodel.mapping.DiscriminatorType;
import org.hibernate.metamodel.mapping.EmbeddableDiscriminatorMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.type.BasicType;

import static org.hibernate.sql.ast.spi.SqlExpressionResolver.createColumnReferenceKey;

/**
 * @author Steve Ebersole
 */
public class ExplicitColumnDiscriminatorMappingImpl extends AbstractDiscriminatorMapping
		implements EmbeddableDiscriminatorMapping {
	private final String name;
	private final String tableExpression;
	private final String columnName;
	private final String columnFormula;
	private final boolean isPhysical;
	private final boolean isUpdateable;
	private final @Nullable String columnDefinition;
	private final @Nullable String customReadExpression;
	private final @Nullable Long length;
	private final @Nullable Integer arrayLength;
	private final @Nullable Integer precision;
	private final @Nullable Integer scale;

	public ExplicitColumnDiscriminatorMappingImpl(
			ManagedMappingType mappingType,
			String name,
			String tableExpression,
			String columnExpression,
			boolean isFormula,
			boolean isPhysical,
			boolean isUpdateable,
			String columnDefinition,
			String customReadExpression,
			Long length,
			Integer precision,
			Integer scale,
			DiscriminatorType<?> discriminatorType) {
		this(
				mappingType,
				name,
				tableExpression,
				columnExpression,
				isFormula,
				isPhysical,
				isUpdateable,
				columnDefinition,
				customReadExpression,
				length,
				null,
				precision,
				scale,
				discriminatorType );
	}

	public ExplicitColumnDiscriminatorMappingImpl(
			ManagedMappingType mappingType,
			String name,
			String tableExpression,
			String columnExpression,
			boolean isFormula,
			boolean isPhysical,
			boolean isUpdateable,
			@Nullable String columnDefinition,
			@Nullable String customReadExpression,
			@Nullable Long length,
			@Nullable Integer arrayLength,
			@Nullable Integer precision,
			@Nullable Integer scale,
			DiscriminatorType<?> discriminatorType) {
		//noinspection unchecked
		super( mappingType, (DiscriminatorType<Object>) discriminatorType, (BasicType<Object>) discriminatorType.getUnderlyingJdbcMapping() );
		this.name = name;
		this.tableExpression = tableExpression;
		this.isPhysical = isPhysical;
		this.columnDefinition = columnDefinition;
		this.customReadExpression = customReadExpression;
		this.length = length;
		this.arrayLength = arrayLength;
		this.precision = precision;
		this.scale = scale;
		if ( isFormula ) {
			columnName = null;
			columnFormula = columnExpression;
			this.isUpdateable = false;
		}
		else {
			columnName = columnExpression;
			columnFormula = null;
			this.isUpdateable = isUpdateable;
		}
	}

	@Override
	public DiscriminatorType<?> getMappedType() {
		return (DiscriminatorType<?>) super.getMappedType();
	}

	@Override
	public DiscriminatorConverter<?, ?> getValueConverter() {
		return getMappedType().getValueConverter();
	}

	@Override
	public Expression resolveSqlExpression(
			NavigablePath navigablePath,
			JdbcMapping jdbcMappingToUse,
			TableGroup tableGroup,
			SqlAstCreationState creationState) {
		final SqlExpressionResolver expressionResolver = creationState.getSqlExpressionResolver();
		final TableReference tableReference = tableGroup.resolveTableReference( navigablePath, tableExpression );

		return expressionResolver.resolveSqlExpression(
				createColumnReferenceKey(
						tableGroup.getPrimaryTableReference(),
						getSelectionExpression(),
						jdbcMappingToUse
				),
				processingState -> new ColumnReference( tableReference, this )
		);
	}

	@Override
	public String getContainingTableExpression() {
		return tableExpression;
	}

	@Override
	public String getSelectableName() {
		return name;
	}

	@Override
	public String getSelectionExpression() {
		return columnName == null ? columnFormula : columnName;
	}

	@Override
	public @Nullable String getCustomReadExpression() {
		return customReadExpression;
	}

	@Override
	public @Nullable String getCustomWriteExpression() {
		return null;
	}

	@Override
	public @Nullable String getColumnDefinition() {
		return columnDefinition;
	}

	@Override
	public @Nullable Long getLength() {
		return length;
	}

	@Override
	public @Nullable Integer getArrayLength() {
		return arrayLength;
	}

	@Override
	public @Nullable Integer getPrecision() {
		return precision;
	}

	@Override
	public @Nullable Integer getScale() {
		return scale;
	}

	@Override
	public @Nullable Integer getTemporalPrecision() {
		return null;
	}

	@Override
	public boolean isFormula() {
		return columnFormula != null;
	}

	@Override
	public boolean isNullable() {
		return false;
	}

	@Override
	public boolean isInsertable() {
		return isPhysical;
	}

	@Override
	public boolean isUpdateable() {
		return isUpdateable;
	}

	@Override
	public boolean isPartitioned() {
		return false;
	}

	@Override
	public boolean hasPartitionedSelectionMapping() {
		return false;
	}

	@Override
	public boolean hasPhysicalColumn() {
		return isPhysical;
	}
}
