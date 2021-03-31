/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.List;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectionConsumer;
import org.hibernate.metamodel.mapping.SelectionMapping;
import org.hibernate.metamodel.mapping.SelectionMappings;

/**
 * @author Steve Ebersole
 */
public class SingleSelectionMappings implements SelectionMapping, SelectionMappings {
	private final String tableName;
	private final String expression;
	private final String readExpression;
	private final String writeExpression;
	private final boolean isFormula;
	private final JdbcMapping jdbcMapping;

	public SingleSelectionMappings(
			String tableName,
			String expression,
			String readExpression,
			String writeExpression,
			boolean isFormula,
			JdbcMapping jdbcMapping) {
		this.tableName = tableName;
		this.expression = expression;
		this.readExpression = readExpression;
		this.writeExpression = writeExpression;
		this.isFormula = isFormula;
		this.jdbcMapping = jdbcMapping;
	}

	@Override
	public String getContainingTableExpression() {
		return tableName;
	}

	@Override
	public String getSelectionExpression() {
		return expression;
	}

	@Override
	public String getCustomReadExpression() {
		return readExpression;
	}

	@Override
	public String getCustomWriteExpression() {
		return writeExpression;
	}

	@Override
	public boolean isFormula() {
		return isFormula;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return jdbcMapping;
	}

	@Override
	public SelectionMapping getSelectionMapping(int columnIndex) {
		return this;
	}

	@Override
	public int getJdbcTypeCount() {
		return 1;
	}

	@Override
	public int forEachSelection(int offset, SelectionConsumer consumer) {
		assert offset == 1;
		consumer.accept( offset, this );
		return 1;
	}

	@Override
	public int forEachSelection(SelectionConsumer consumer) {
		consumer.accept( 0, this );
		return 1;
	}

	@Override
	public List<JdbcMapping> getJdbcMappings() {
		return null;
	}
}
