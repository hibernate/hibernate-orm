/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.sql.results.spi.DynamicInstantiationQueryResult;
import org.hibernate.sql.results.spi.InitializerCollector;
import org.hibernate.sql.results.spi.QueryResultAssembler;

/**
 * @author Steve Ebersole
 */
public class DynamicInstantiationQueryResultImpl implements DynamicInstantiationQueryResult {
	private final String resultVariable;
	private final QueryResultAssembler assembler;

	public DynamicInstantiationQueryResultImpl(
			String resultVariable,
			QueryResultAssembler assembler) {
		this.resultVariable = resultVariable;
		this.assembler = assembler;
	}

	@Override
	public String getResultVariable() {
		return resultVariable;
	}

	@Override
	public void registerInitializers(InitializerCollector collector) {
		// todo (6.0) : do we need to register initializers for any of the arguments?
	}

	@Override
	public QueryResultAssembler getResultAssembler() {
		return assembler;
	}
}
