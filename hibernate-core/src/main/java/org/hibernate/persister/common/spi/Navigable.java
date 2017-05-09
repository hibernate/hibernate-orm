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
import org.hibernate.sql.ast.produce.result.spi.Return;
import org.hibernate.sql.ast.produce.result.spi.ReturnResolutionContext;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Models a "piece" of the application's domain model that can be navigated
 * as part of a query or the NavigableVisitationStrategy contract.
 *
 * @author Steve Ebersole
 */
public interface Navigable<T> extends ExpressableType<T>, TypeExporter<T> {
	NavigableSource getSource();

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

	Return generateReturn(ReturnResolutionContext returnResolutionContext, TableGroup tableGroup);

	Fetch generateFetch(ReturnResolutionContext returnResolutionContext, TableGroup tableGroup, FetchParent fetchParent);
}
