/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.spi;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.sql.results.spi.AssemblerCreationContext;
import org.hibernate.sql.results.spi.DomainResultCreationContext;

/**
 * The "context" in which creation of SQL AST occurs.
 *
 * @author Steve Ebersole
 */
public interface SqlAstCreationContext extends AssemblerCreationContext, DomainResultCreationContext {
	SqlExpressionResolver getSqlSelectionResolver();

	LoadQueryInfluencers getLoadQueryInfluencers();

	default boolean shouldCreateShallowEntityResult() {
		return false;
	}

	LockOptions getLockOptions();
}
