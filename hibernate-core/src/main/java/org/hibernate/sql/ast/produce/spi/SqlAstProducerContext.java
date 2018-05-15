/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.spi;

import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.produce.sqm.spi.Callback;

/**
 * An encapsulation of things needed while building a SQL AST
 *
 * @author Steve Ebersole
 */
public interface SqlAstProducerContext {
	SessionFactoryImplementor getSessionFactory();

	/**
	 * todo (6.0) Consider instead defining access to the root AttributeNodeContainer
	 */
	LoadQueryInfluencers getLoadQueryInfluencers();

	Callback getCallback();
}
