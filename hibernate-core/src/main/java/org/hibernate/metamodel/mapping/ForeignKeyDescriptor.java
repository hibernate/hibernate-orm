/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;

/**
 * @author Steve Ebersole
 */
public interface ForeignKeyDescriptor extends VirtualModelPart {
	String PART_NAME = "{fk}";

	DomainResult createCollectionFetchDomainResult(
			NavigablePath collectionPath,
			TableGroup tableGroup,
			DomainResultCreationState creationState);

	DomainResult createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState);

	DomainResult createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			boolean isKeyReferringSide,
			DomainResultCreationState creationState);

	Predicate generateJoinPredicate(
			TableGroup lhs,
			TableGroup tableGroup,
			SqlAstJoinType sqlAstJoinType,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext);

	Predicate generateJoinPredicate(
			TableReference lhs,
			TableReference rhs,
			SqlAstJoinType sqlAstJoinType,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext);

	@Override
	default String getPartName() {
		return PART_NAME;
	}

	/**
	 * Visits the FK "referring" columns
	 */
	@Override
	default int forEachSelection(int offset, SelectionConsumer consumer) {
		return visitReferringColumns( offset, consumer );
	}

	int visitReferringColumns(int offset, SelectionConsumer consumer);

	int visitTargetColumns(int offset, SelectionConsumer consumer);

	default int visitReferringColumns(SelectionConsumer consumer)  {
		return visitReferringColumns( 0, consumer );
	}

	default int visitTargetColumns(SelectionConsumer consumer) {
		return visitTargetColumns( 0, consumer );
	}

	AssociationKey getAssociationKey();
}
