/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results;

import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.type.descriptor.ValueExtractor;

/**
 * SqlSelection for NativeQuery
 *
 * @author Steve Ebersole
 */
public class SqlSelectionImpl implements SqlSelection {
	private final int valuesArrayPosition;
	private final BasicValuedMapping valueMapping;

	public SqlSelectionImpl(int valuesArrayPosition, BasicValuedMapping valueMapping) {
		this.valuesArrayPosition = valuesArrayPosition;
		this.valueMapping = valueMapping;
	}

	@Override
	public ValueExtractor getJdbcValueExtractor() {
		return valueMapping.getJdbcMapping().getJdbcValueExtractor();
	}

	@Override
	public int getValuesArrayPosition() {
		return valuesArrayPosition;
	}

	@Override
	public MappingModelExpressable getExpressionType() {
		return valueMapping;
	}

	@Override
	public void accept(SqlAstWalker sqlAstWalker) {
		throw new UnsupportedOperationException();
	}
}
