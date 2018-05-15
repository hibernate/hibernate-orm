/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.tree.SqmUpdateStatement;
import org.hibernate.sql.exec.spi.ParameterBindingContext;

/**
 * @author Steve Ebersole
 */
public class SimpleUpdateQueryPlan implements NonSelectQueryPlan {
	private final SqmUpdateStatement sqmStatement;

	public SimpleUpdateQueryPlan(SqmUpdateStatement sqmStatement) {
		this.sqmStatement = sqmStatement;

		// todo (6.0) : here is where we need to perform the conversion into SQL AST
	}

	@Override
	public int executeUpdate(
			SharedSessionContractImplementor persistenceContext,
			QueryOptions queryOptions,
			ParameterBindingContext parameterBindingContext) {

		throw new NotYetImplementedFor6Exception(  );
	}
}
