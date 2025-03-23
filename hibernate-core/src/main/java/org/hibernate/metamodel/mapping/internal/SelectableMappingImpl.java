/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.Locale;

import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Selectable;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Christian Beikov
 */
public class SelectableMappingImpl extends SqlTypedMappingImpl implements SelectableMapping {

	private final String containingTableExpression;
	private final String selectionExpression;
	private final SelectablePath selectablePath;
	private final String customReadExpression;
	private final String customWriteExpression;
	private final boolean isLob;
	private final boolean nullable;
	private final boolean insertable;
	private final boolean updateable;
	private final boolean partitioned;
	private final boolean isFormula;

	public SelectableMappingImpl(
			String containingTableExpression,
			String selectionExpression,
			SelectablePath selectablePath,
			String customReadExpression,
			String customWriteExpression,
			String columnDefinition,
			Long length,
			Integer precision,
			Integer scale,
			Integer temporalPrecision,
			boolean isLob,
			boolean nullable,
			boolean insertable,
			boolean updateable,
			boolean partitioned,
			boolean isFormula,
			JdbcMapping jdbcMapping) {
		super( columnDefinition, length, precision, scale, temporalPrecision, jdbcMapping );
		assert selectionExpression != null;
		// Save memory by using interned strings. Probability is high that we have multiple duplicate strings
		this.containingTableExpression = containingTableExpression == null ? null : containingTableExpression.intern();
		this.selectionExpression = selectionExpression.intern();
		this.selectablePath = selectablePath == null ? new SelectablePath( selectionExpression ) : selectablePath;
		this.customReadExpression = customReadExpression == null ? null : customReadExpression.intern();
		this.customWriteExpression = customWriteExpression == null || isFormula ? null : customWriteExpression.intern();
		this.isLob = isLob;
		this.nullable = nullable;
		this.insertable = insertable;
		this.updateable = updateable;
		this.partitioned = partitioned;
		this.isFormula = isFormula;
	}

	public static SelectableMapping from(
			final String containingTableExpression,
			final Selectable selectable,
			final JdbcMapping jdbcMapping,
			final TypeConfiguration typeConfiguration,
			boolean insertable,
			boolean updateable,
			boolean partitioned,
			final Dialect dialect,
			final SqmFunctionRegistry sqmFunctionRegistry,
			RuntimeModelCreationContext creationContext) {
		return from(
				containingTableExpression,
				selectable,
				null,
				jdbcMapping,
				typeConfiguration,
				insertable,
				updateable,
				partitioned,
				dialect,
				sqmFunctionRegistry,
				creationContext
		);
	}

	public static SelectableMapping from(
			final String containingTableExpression,
			final Selectable selectable,
			final JdbcMapping jdbcMapping,
			final TypeConfiguration typeConfiguration,
			boolean insertable,
			boolean updateable,
			boolean partitioned,
			boolean forceNotNullable,
			final Dialect dialect,
			final SqmFunctionRegistry sqmFunctionRegistry,
			RuntimeModelCreationContext creationContext) {
		return from(
				containingTableExpression,
				selectable,
				null,
				jdbcMapping,
				typeConfiguration,
				insertable,
				updateable,
				partitioned,
				forceNotNullable,
				dialect,
				sqmFunctionRegistry,
				creationContext
		);
	}

	public static SelectableMapping from(
			final String containingTableExpression,
			final Selectable selectable,
			final SelectablePath parentPath,
			final JdbcMapping jdbcMapping,
			final TypeConfiguration typeConfiguration,
			boolean insertable,
			boolean updateable,
			boolean partitioned,
			final Dialect dialect,
			final SqmFunctionRegistry sqmFunctionRegistry,
			RuntimeModelCreationContext creationContext) {
		return from(
				containingTableExpression,
				selectable,
				parentPath,
				jdbcMapping,
				typeConfiguration,
				insertable,
				updateable,
				partitioned,
				false,
				dialect,
				sqmFunctionRegistry,
				creationContext
		);
	}

	public static SelectableMapping from(
			final String containingTableExpression,
			final Selectable selectable,
			final SelectablePath parentPath,
			final JdbcMapping jdbcMapping,
			final TypeConfiguration typeConfiguration,
			boolean insertable,
			boolean updateable,
			boolean partitioned,
			boolean forceNotNullable,
			final Dialect dialect,
			final SqmFunctionRegistry sqmFunctionRegistry,
			RuntimeModelCreationContext creationContext) {
		final String columnExpression;
		final String columnDefinition;
		final Long length;
		final Integer precision;
		final Integer scale;
		final Integer temporalPrecision;
		final String selectableName;
		final boolean isLob;
		final boolean isNullable;
		if ( selectable.isFormula() ) {
			columnExpression = selectable.getTemplate( dialect, typeConfiguration );
			columnDefinition = null;
			length = null;
			precision = null;
			scale = null;
			temporalPrecision = null;
			isNullable = true;
			isLob = false;
			selectableName = selectable.getText();
		}
		else {
			Column column = (Column) selectable;
			columnExpression = selectable.getText( dialect );
			columnDefinition = column.getSqlType();
			length = column.getLength();
			precision = column.getPrecision();
			scale = column.getScale();
			temporalPrecision = column.getTemporalPrecision();

			isNullable = !forceNotNullable && column.isNullable();
			isLob = column.isSqlTypeLob( creationContext.getMetadata() );
			selectableName = column.getQuotedName( dialect );
		}
		return new SelectableMappingImpl(
				containingTableExpression,
				columnExpression,
				parentPath == null
						? null
						: parentPath.append( selectableName ),
				selectable.getCustomReadExpression(),
				selectable.getWriteExpr( jdbcMapping, dialect ),
				columnDefinition,
				length,
				precision,
				scale,
				temporalPrecision,
				isLob,
				isNullable,
				insertable,
				updateable,
				partitioned,
				selectable.isFormula(),
				jdbcMapping
		);
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"SelectableMapping(`%s`.`%s`)",
				containingTableExpression,
				selectionExpression
		);
	}

	@Override
	public String getContainingTableExpression() {
		return containingTableExpression;
	}

	@Override
	public String getSelectionExpression() {
		return selectionExpression;
	}

	@Override
	public String getSelectableName() {
		return selectablePath == null ? null : selectablePath.getSelectableName();
	}

	@Override
	public SelectablePath getSelectablePath() {
		return selectablePath;
	}

	@Override
	public String getCustomReadExpression() {
		return customReadExpression;
	}

	@Override
	public String getCustomWriteExpression() {
		return customWriteExpression;
	}

	@Override
	public String getWriteExpression() {
		return customWriteExpression;
	}

	@Override
	public boolean isLob() {
		return isLob;
	}

	@Override
	public boolean isFormula() {
		return isFormula;
	}

	@Override
	public boolean isNullable() {
		return nullable;
	}

	@Override
	public boolean isInsertable() {
		return insertable;
	}

	@Override
	public boolean isUpdateable() {
		return updateable;
	}

	@Override
	public boolean isPartitioned() {
		return partitioned;
	}
}
