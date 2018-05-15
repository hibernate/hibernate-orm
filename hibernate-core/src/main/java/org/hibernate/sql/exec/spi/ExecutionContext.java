/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.spi;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.produce.sqm.spi.Callback;

/**
 * Contextual information needed while executing some JDBC operation,
 * generally including conversion from SQL AST to JdbcOperation(s)
 *
 * @author Steve Ebersole
 */
//public interface ExecutionContext extends QueryResultAssemblerCreationContext {
public interface ExecutionContext {
	default Object resolveEntityInstance(EntityKey entityKey, boolean eager) {
		return StandardEntityInstanceResolver.resolveEntityInstance(
				entityKey,
				eager,
				getSession()
		);
	}

	SharedSessionContractImplementor getSession();

//	@Override
//	default SessionFactoryImplementor getSessionFactory() {
//		return getSession().getSessionFactory();
//	}

	QueryOptions getQueryOptions();

//	@Override
//	default LoadQueryInfluencers getLoadQueryInfluencers() {
//		return getSession().getLoadQueryInfluencers();
//	}

	// todo (6.0) : ParameterBindingContext is not needed here, although should be available via SqlAstCreationContext
	//		here, should just be JdbcParameterBindings and possibly a list of JdbcParameters

	ParameterBindingContext getParameterBindingContext();

	// todo (6.0) : this should go away also - it makes the execution context specific to an execution
	//		see QuerySqmImpl implementing ExecutionContext as an example.. the ExecutionContext is
	//		really scoped to the execution method (`#doScroll`, etc)

	default JdbcParameterBindings getJdbcParameterBindings() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	Callback getCallback();
}
