/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

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
	private final String columnDefinition;
	private final String customReadExpression;
	private final Long length;
	private final Integer precision;
	private final Integer scale;

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
		//noinspection unchecked
		super( mappingType, (DiscriminatorType<Object>) discriminatorType, (BasicType<Object>) discriminatorType.getUnderlyingJdbcMapping() );
		this.name = name;
		this.tableExpression = tableExpression;
		this.isPhysical = isPhysical;
		this.columnDefinition = columnDefinition;
		this.customReadExpression = customReadExpression;
		this.length = length;
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
	public DiscriminatorType getMappedType() {
		return (DiscriminatorType) super.getMappedType();
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
	public String getCustomReadExpression() {
		return customReadExpression;
	}

	@Override
	public String getCustomWriteExpression() {
		return null;
	}

	@Override
	public String getColumnDefinition() {
		return columnDefinition;
	}

	@Override
	public Long getLength() {
		return length;
	}

	@Override
	public Integer getPrecision() {
		return precision;
	}

	@Override
	public Integer getScale() {
		return scale;
	}

	@Override
	public Integer getTemporalPrecision() {
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
