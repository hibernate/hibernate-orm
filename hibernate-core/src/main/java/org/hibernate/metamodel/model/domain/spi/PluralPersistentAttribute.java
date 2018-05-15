/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableContainerReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmPluralAttributeReference;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.metamodel.spi.Fetchable;
import org.hibernate.sql.ast.produce.metamodel.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.produce.metamodel.spi.TableGroupInfo;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.JoinedTableGroupContext;
import org.hibernate.sql.ast.produce.spi.RootTableGroupContext;
import org.hibernate.sql.ast.produce.spi.RootTableGroupProducer;
import org.hibernate.sql.ast.produce.spi.SqlAliasBase;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.produce.spi.TableGroupJoinProducer;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableContainerReference;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.spi.from.TableSpace;
import org.hibernate.type.descriptor.java.internal.CollectionJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public interface PluralPersistentAttribute<O,C,E>
		extends NonIdPersistentAttribute<O,C>, CollectionValuedNavigable<C>,
		javax.persistence.metamodel.PluralAttribute<O,C,E>, Fetchable<C>, RootTableGroupProducer, TableGroupJoinProducer {

	PersistentCollectionDescriptor<O,C,E> getPersistentCollectionDescriptor();

	@Override
	CollectionJavaDescriptor<C> getJavaTypeDescriptor();

	@Override
	Class<C> getJavaType();

	@Override
	default void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitPluralAttribute( this );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Delegations to PersistentCollectionDescriptor

	@Override
	default PersistentCollectionDescriptor getCollectionDescriptor() {
		return getPersistentCollectionDescriptor();
	}

	@Override
	@SuppressWarnings("unchecked")
	default <N> Navigable<N> findNavigable(String navigableName) {
		return getCollectionDescriptor().findNavigable( navigableName );
	}

	@Override
	default void visitNavigables(NavigableVisitationStrategy visitor) {
		getCollectionDescriptor().visitNavigables( visitor );
	}

	@Override
	default void visitKeyFetchables(Consumer<Fetchable> fetchableConsumer) {
		getCollectionDescriptor().visitKeyFetchables( fetchableConsumer );
	}

	@Override
	default void visitFetchables(Consumer<Fetchable> fetchableConsumer) {
		getCollectionDescriptor().visitFetchables( fetchableConsumer );
	}

	@Override
	default void applyTableReferenceJoins(
			ColumnReferenceQualifier lhs,
			JoinType joinType,
			SqlAliasBase sqlAliasBase,
			TableReferenceJoinCollector joinCollector) {
		getCollectionDescriptor().applyTableReferenceJoins( lhs, joinType, sqlAliasBase, joinCollector );
	}

	@Override
	default PersistenceType getPersistenceType() {
		return getPersistentCollectionDescriptor().getPersistenceType();
	}

	@Override
	default CollectionType getCollectionType() {
		return getPersistentCollectionDescriptor().getCollectionClassification().toJpaClassification();
	}

	@Override
	default SimpleTypeDescriptor<E> getElementType() {
		return (SimpleTypeDescriptor<E>) getPersistentCollectionDescriptor().getElementDescriptor();
	}

	@Override
	SqmPluralAttributeReference createSqmExpression(
			SqmFrom sourceSqmFrom,
			SqmNavigableContainerReference containerReference,
			SqmCreationContext creationContext);

	@Override
	default TableGroupJoin createTableGroupJoin(
			TableGroupInfo tableGroupInfoSource,
			JoinType joinType,
			JoinedTableGroupContext tableGroupJoinContext) {
		return getPersistentCollectionDescriptor().createTableGroupJoin( tableGroupInfoSource, joinType, tableGroupJoinContext );
	}

	@Override
	default TableGroupJoin createTableGroupJoin(
			SqlAliasBaseGenerator sqlAliasBaseGenerator,
			NavigableContainerReference lhs,
			SqlExpressionResolver sqlExpressionResolver,
			NavigablePath navigablePath,
			JoinType joinType,
			String identificationVariable,
			LockMode lockMode,
			TableSpace tableSpace) {
		return getPersistentCollectionDescriptor().createTableGroupJoin(
				sqlAliasBaseGenerator,
				lhs,
				sqlExpressionResolver,
				navigablePath,
				joinType,
				identificationVariable,
				lockMode,
				tableSpace
		);
	}

	@Override
	default String getSqlAliasStem() {
		return getPersistentCollectionDescriptor().getSqlAliasStem();
	}

	@Override
	default TableGroup createRootTableGroup(TableGroupInfo tableGroupInfo, RootTableGroupContext tableGroupContext) {
		return getPersistentCollectionDescriptor().createRootTableGroup( tableGroupInfo, tableGroupContext );
	}
}
