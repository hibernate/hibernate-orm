/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.multitable.spi;

import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.produce.spi.SqlAstProducerContext;
import org.hibernate.sql.exec.spi.ExecutionContext;

/**
 * @author Steve Ebersole
 */
public interface HandlerExecutionContext extends ExecutionContext, SqlAstProducerContext {
	@Override
	default SessionFactoryImplementor getSessionFactory() {
		return getSession().getFactory();
	}

	@Override
	default LoadQueryInfluencers getLoadQueryInfluencers() {
		return getSession().getLoadQueryInfluencers();
	}
}
