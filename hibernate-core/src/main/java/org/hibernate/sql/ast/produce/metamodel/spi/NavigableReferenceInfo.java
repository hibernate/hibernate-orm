/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.metamodel.spi;

import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.NavigablePath;

/**
 * Access to "source" information about a NavigableReference to be
 * built - a set of parameters indicating information to encode into the
 * NavigableReference - a "parameter object" for methods generating these
 * NavigableReferences.
 *
 * This information varies depending on the particular source (HQL, loading, etc).
 *
 * @author Steve Ebersole
 *
 * @apiNote The point of this contract is to isolate the different sources
 * of SQL AST trees (HQL, NativeQuery, LoadPlan), especially around producing
 * TableGroups and NavigableReferences ({@link NavigableReferenceInfo}).
 */
public interface NavigableReferenceInfo extends TableGroupInfo, ExpressableType {
	NavigableContainerReferenceInfo getNavigableContainerReferenceInfo();

	Navigable getReferencedNavigable();

	NavigablePath getNavigablePath();
}
