/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.result.internal;

import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.instantiation.DynamicInstantiation;
import org.hibernate.sql.ast.produce.result.spi.QueryResultDynamicInstantiation;
import org.hibernate.sql.ast.consume.results.spi.QueryResultAssembler;

/**
 * @author Steve Ebersole
 */
public class QueryResultDynamicInstantiationImpl implements QueryResultDynamicInstantiation {
	private final DynamicInstantiation dynamicInstantiation;
	private final String resultVariable;
	private final QueryResultAssembler assembler;

	public QueryResultDynamicInstantiationImpl(
			DynamicInstantiation dynamicInstantiation,
			String resultVariable,
			QueryResultAssembler assembler) {
		this.dynamicInstantiation = dynamicInstantiation;
		this.resultVariable = resultVariable;
		this.assembler = assembler;
	}

	@Override
	public Class getInstantiationTarget() {
		return dynamicInstantiation.getTarget();
	}

	@Override
	public Expression getSelectedExpression() {
		return dynamicInstantiation;
	}

	@Override
	public String getResultVariable() {
		return resultVariable;
	}

	@Override
	public Class getReturnedJavaType() {
		return dynamicInstantiation.getTarget();
	}

	@Override
	public QueryResultAssembler getResultAssembler() {
		return assembler;
	}
}
