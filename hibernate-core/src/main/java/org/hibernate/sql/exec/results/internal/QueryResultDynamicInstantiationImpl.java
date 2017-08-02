/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.results.internal;

import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.tree.spi.expression.instantiation.DynamicInstantiation;
import org.hibernate.sql.exec.results.spi.QueryResultAssembler;
import org.hibernate.sql.exec.results.spi.QueryResultDynamicInstantiation;

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

	public DynamicInstantiation getDynamicInstantiation() {
		return dynamicInstantiation;
	}

	@Override
	public String getResultVariable() {
		return resultVariable;
	}

	@Override
	public ExpressableType getType() {
		return dynamicInstantiation.getType();
	}

	@Override
	public QueryResultAssembler getResultAssembler() {
		return assembler;
	}
}
