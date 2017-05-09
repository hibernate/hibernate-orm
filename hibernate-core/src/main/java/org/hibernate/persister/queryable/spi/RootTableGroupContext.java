/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.queryable.spi;

import org.hibernate.sql.ast.tree.spi.from.TableSpace;
import org.hibernate.sql.ast.tree.spi.predicate.Predicate;

/**
 * Parameter object passed to {@link RootTableGroupProducer#applyTableGroup} giving
 * mutation access to the context into which the TableGroup is being applied.  This
 * gives the producer a chance to alter that context, e.g. to add additional
 * restrictions for filters.
 *
 * @author Steve Ebersole
 */
public interface RootTableGroupContext {
	TableSpace getTableSpace();
	void addRestriction(Predicate predicate);
}
