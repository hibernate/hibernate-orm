/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.common.spi;

import org.hibernate.persister.common.NavigableRole;
import org.hibernate.persister.queryable.spi.ExpressableType;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.produce.result.spi.Fetch;
import org.hibernate.sql.ast.produce.result.spi.FetchParent;
import org.hibernate.sql.ast.produce.result.spi.QueryResult;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Models a "piece" of the application's domain model that can be navigated
 * as part of a query or the NavigableVisitationStrategy contract.
 *
 * @author Steve Ebersole
 */
public interface Navigable<T> extends ExpressableType<T>, TypeExporter<T> {
	/**
	 * The NavigableContainer which contains this Navigable.  A NavigableContainer
	 * is container for
	 */
	NavigableContainer getContainer();

	NavigableRole getNavigableRole();

	default String getNavigableName() {
		return getNavigableRole().getNavigableName();
	}

	JavaTypeDescriptor getJavaTypeDescriptor();

	/**
	 * Obtain a loggable representation.
	 *
	 * @return The loggable representation of this reference
	 */
	String asLoggableText();

	void visitNavigable(NavigableVisitationStrategy visitor);

	QueryResult generateReturn(QueryResultCreationContext returnResolutionContext, TableGroup tableGroup);

	Fetch generateFetch(QueryResultCreationContext returnResolutionContext, TableGroup tableGroup, FetchParent fetchParent);
}
