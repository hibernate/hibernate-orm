/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.metamodel.mapping.DiscriminatorConverter;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.DiscriminatorType;
import org.hibernate.metamodel.mapping.MappingType;
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
public class ExplicitColumnDiscriminatorMappingImpl extends AbstractDiscriminatorMapping {
	private final String tableExpression;
	private final String columnName;
	private final String columnFormula;
	private final boolean isPhysical;
	private final String columnDefinition;
	private final Long length;
	private final Integer precision;
	private final Integer scale;

	public ExplicitColumnDiscriminatorMappingImpl(
			EntityMappingType entityDescriptor,
			String tableExpression,
			String columnExpression,
			boolean isFormula,
			boolean isPhysical,
			String columnDefinition,
			Long length,
			Integer precision,
			Integer scale,
			DiscriminatorType<?> discriminatorType,
			MappingModelCreationProcess creationProcess) {
		//noinspection unchecked
		super( entityDescriptor, (DiscriminatorType<Object>) discriminatorType, (BasicType<Object>) discriminatorType.getUnderlyingJdbcMapping() );
		this.tableExpression = tableExpression;
		this.isPhysical = isPhysical;
		this.columnDefinition = columnDefinition;
		this.length = length;
		this.precision = precision;
		this.scale = scale;
		if ( isFormula ) {
			columnName = null;
			columnFormula = columnExpression;
		}
		else {
			columnName = columnExpression;
			columnFormula = null;
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
	public String getSelectionExpression() {
		return columnName == null ? columnFormula : columnName;
	}

	@Override
	public String getCustomReadExpression() {
		return null;
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
		return false;
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
