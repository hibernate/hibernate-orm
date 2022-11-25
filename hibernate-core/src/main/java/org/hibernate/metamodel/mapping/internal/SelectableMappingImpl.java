/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.Locale;

import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Selectable;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Christian Beikov
 */
public class SelectableMappingImpl extends SqlTypedMappingImpl implements SelectableMapping {

	private final String containingTableExpression;
	private final String selectionExpression;
	private final String customReadExpression;
	private final String customWriteExpression;
	private final boolean nullable;
	private final boolean insertable;
	private final boolean updateable;
	private final boolean isFormula;

	public SelectableMappingImpl(
			String containingTableExpression,
			String selectionExpression,
			String customReadExpression,
			String customWriteExpression,
			String columnDefinition,
			Long length,
			Integer precision,
			Integer scale,
			boolean nullable,
			boolean insertable,
			boolean updateable,
			boolean isFormula,
			JdbcMapping jdbcMapping) {
		super( columnDefinition, length, precision, scale, jdbcMapping );
		// Save memory by using interned strings. Probability is high that we have multiple duplicate strings
		this.containingTableExpression = containingTableExpression == null ? null : containingTableExpression.intern();
		this.selectionExpression = selectionExpression == null ? null : selectionExpression.intern();
		this.customReadExpression = customReadExpression == null ? null : customReadExpression.intern();
		this.customWriteExpression = customWriteExpression == null ? null : customWriteExpression.intern();
		this.nullable = nullable;
		this.insertable = insertable;
		this.updateable = updateable;
		this.isFormula = isFormula;
	}

	public static SelectableMapping from(
			final String containingTableExpression,
			final Selectable selectable,
			final JdbcMapping jdbcMapping,
			final TypeConfiguration typeConfiguration,
			boolean insertable,
			boolean updateable,
			final Dialect dialect,
			final SqmFunctionRegistry sqmFunctionRegistry) {
		final String columnExpression;
		final String columnDefinition;
		final Long length;
		final Integer precision;
		final Integer scale;
		final boolean isNullable;
		if ( selectable.isFormula() ) {
			columnExpression = selectable.getTemplate( dialect, typeConfiguration, sqmFunctionRegistry );
			columnDefinition = null;
			length = null;
			precision = null;
			scale = null;
			isNullable = true;
		}
		else {
			Column column = (Column) selectable;
			columnExpression = selectable.getText( dialect );
			columnDefinition = column.getSqlType();
			length = column.getLength();
			precision = column.getPrecision();
			scale = column.getScale();

			isNullable = column.isNullable();
		}
		return new SelectableMappingImpl(
				containingTableExpression,
				columnExpression,
				selectable.getCustomReadExpression(),
				selectable.getCustomWriteExpression(),
				columnDefinition,
				length,
				precision,
				scale,
				isNullable,
				insertable,
				updateable,
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
	public String getCustomReadExpression() {
		return customReadExpression;
	}

	@Override
	public String getCustomWriteExpression() {
		return customWriteExpression;
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
}
