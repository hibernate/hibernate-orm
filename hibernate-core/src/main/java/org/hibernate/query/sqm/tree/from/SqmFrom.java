/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import org.hibernate.persister.queryable.spi.TableGroupInfoSource;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;

/**
 * Models a Bindable's inclusion in the {@code FROM} clause.
 *
 * @author Steve Ebersole
 */
public interface SqmFrom extends TableGroupInfoSource {
	/**
	 * Obtain reference to the FromElementSpace that this FromElement belongs to.
	 */
	SqmFromElementSpace getContainingSpace();

	SqmNavigableReference getNavigableReference();

	<T> T accept(SemanticQueryWalker<T> walker);
}
