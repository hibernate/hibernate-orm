/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.collection;

import org.hibernate.metamodel.model.domain.spi.CollectionElement;
import org.hibernate.metamodel.model.domain.spi.CollectionIndex;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.produce.spi.FromClauseAccess;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;

/**
 * Helper for generation of SQL AST nodes relating to collections
 *
 * @author Steve Ebersole
 */
public class SqlAstHelper {
	public static DomainResult generateCollectionElementDomainResult(
			NavigablePath elementPath,
			CollectionElement elementDescriptor,
			boolean isSelected,
			String resultVariable,
			DomainResultCreationState creationState) {
		assert elementDescriptor != null;
		assert elementPath != null;
		assert elementPath.getParent() != null;
		assert elementPath.getLocalName().equals( CollectionElement.NAVIGABLE_NAME );

		// make sure the TableGroup to use for the elements is registered

		final FromClauseAccess fromClauseAccess = creationState.getFromClauseAccess();

		// todo (6.0) : still need to figure out how we want this designed
		//		1) are the index/element tables incorporated into the collection TableGroup?
		//		2) or are they separate TableGroups (possibly, depending on nature) and we create them here.
		//
		// atm, I believe they are all bundled together, so just reuse the collection TableGroup...

		fromClauseAccess.resolveTableGroup(
				elementPath,
				np -> fromClauseAccess.getTableGroup( elementPath.getParent() )
		);

		return elementDescriptor.createDomainResult(
				elementPath,
				resultVariable,
				creationState
		);
	}

	public static DomainResult generateCollectionIndexDomainResult(
			NavigablePath indexPath,
			CollectionIndex indexDescriptor,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {
		assert indexDescriptor != null;
		assert indexPath != null;
		assert indexPath.getParent() != null;
		assert indexPath.getLocalName().equals( CollectionIndex.NAVIGABLE_NAME );

		// make sure the TableGroup to use for the index is registered

		final FromClauseAccess fromClauseAccess = creationState.getFromClauseAccess();

		// todo (6.0) : still need to figure out how we want this designed
		//		1) are the index/element tables incorporated into the collection TableGroup?
		//		2) or are they separate TableGroups (possibly, depending on nature) and we create them here.
		//
		// atm, I believe they are all bundled together, so just reuse the collection TableGroup...

		fromClauseAccess.resolveTableGroup(
				indexPath,
				np -> fromClauseAccess.getTableGroup( indexPath.getParent() )
		);

		return indexDescriptor.createDomainResult(
				indexPath,
				resultVariable,
				creationState
		);
	}
}
