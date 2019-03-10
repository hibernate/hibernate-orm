/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.LockMode;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmPluralValuedSimplePath;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.spi.RootTableGroupProducer;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationState;
import org.hibernate.sql.ast.produce.spi.TableGroupJoinProducer;
import org.hibernate.sql.ast.produce.spi.TableReferenceContributor;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableGroupJoin;

/**
 * @author Steve Ebersole
 */
public interface PluralValuedNavigable<J>
		extends NavigableContainer<J>, TableReferenceContributor, RootTableGroupProducer, TableGroupJoinProducer {
	PersistentCollectionDescriptor getCollectionDescriptor();

	@Override
	default SqmNavigableReference createSqmExpression(SqmPath lhs, SqmCreationState creationState) {
		return new SqmPluralValuedSimplePath(
				creationState.generateUniqueIdentifier(),
				lhs.getNavigablePath().append( getNavigableName() ),
				this,
				lhs,
				null
		);
	}

	@Override
	default TableGroup createRootTableGroup(
			String uid,
			NavigablePath navigablePath,
			String explicitSourceAlias,
			JoinType tableReferenceJoinType,
			LockMode lockMode,
			SqlAstCreationState creationState) {
		// the root form can be safely delegated to the collection descriptor
		return getCollectionDescriptor().createRootTableGroup(
				uid,
				navigablePath,
				explicitSourceAlias,
				tableReferenceJoinType,
				lockMode,
				creationState
		);
	}

	@Override
	default TableGroupJoin createTableGroupJoin(
			String uid,
			NavigablePath navigablePath,
			TableGroup lhs,
			String explicitSourceAlias,
			JoinType joinType,
			LockMode lockMode,
			SqlAstCreationState creationState) {
		// the root form can be safely delegated to the collection descriptor
		return getCollectionDescriptor().createTableGroupJoin(
				uid,
				navigablePath,
				lhs,
				explicitSourceAlias,
				joinType,
				lockMode,
				creationState
		);
	}
}
