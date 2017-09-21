/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.List;

import org.hibernate.mapping.IndexedCollection;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.spi.AbstractCollectionIndex;
import org.hibernate.metamodel.model.domain.spi.CollectionIndexEntity;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.TableReferenceJoinCollector;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.SqlAliasBase;
import org.hibernate.sql.ast.produce.spi.TableGroupContext;
import org.hibernate.sql.ast.tree.spi.expression.domain.EntityReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.results.internal.EntityQueryResultImpl;
import org.hibernate.sql.results.internal.EntitySqlSelectionMappingsBuilder;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public class CollectionIndexEntityImpl<J>
		extends AbstractCollectionIndex<J>
		implements CollectionIndexEntity<J> {
	private final EntityDescriptor<J> entityPersister;
	private final NavigableRole navigableRole;

	public CollectionIndexEntityImpl(
			PersistentCollectionDescriptor persister,
			IndexedCollection mappingBinding,
			RuntimeModelCreationContext creationContext) {
		super( persister );

		this.entityPersister = null;
		this.navigableRole = persister.getNavigableRole().append( NAVIGABLE_NAME );
	}

	@Override
	public EntityDescriptor<J> getEntityDescriptor() {
		return entityPersister;
	}

	@Override
	public String getEntityName() {
		return getEntityDescriptor().getEntityName();
	}

	@Override
	public String getJpaEntityName() {
		return getEntityDescriptor().getJpaEntityName();
	}

	@Override
	public IndexClassification getClassification() {
		// todo : distinguish between OneToMany and ManyToMany
		return IndexClassification.ONE_TO_MANY;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public <N> Navigable<N> findNavigable(String navigableName) {
		return getEntityDescriptor().findNavigable( navigableName );
	}

	@Override
	public <N> Navigable<N> findDeclaredNavigable(String navigableName) {
		return getEntityDescriptor().findDeclaredNavigable( navigableName );
	}

	@Override
	public List<Navigable> getNavigables() {
		return getEntityDescriptor().getNavigables();
	}

	@Override
	public List<Navigable> getDeclaredNavigables() {
		return getEntityDescriptor().getDeclaredNavigables();
	}

	@Override
	public void visitNavigables(NavigableVisitationStrategy visitor) {
		getEntityDescriptor().visitNavigables( visitor );
	}

	@Override
	public void visitDeclaredNavigables(NavigableVisitationStrategy visitor) {
		getEntityDescriptor().visitDeclaredNavigables( visitor );
	}

	@Override
	public List<Column> getColumns() {
		throw new NotYetImplementedFor6Exception(  );
	}

	@Override
	public EntityJavaDescriptor<J> getJavaTypeDescriptor() {
		return getEntityDescriptor().getJavaTypeDescriptor();
	}

	@Override
	public QueryResult createQueryResult(
			NavigableReference navigableReference,
			String resultVariable,
			QueryResultCreationContext creationContext) {
		assert navigableReference instanceof EntityReference;
		final EntityReference entityReference = (EntityReference) navigableReference;

		return new EntityQueryResultImpl(
				entityReference.getNavigable(),
				resultVariable,
				EntitySqlSelectionMappingsBuilder.buildSqlSelectionMappings(
						getEntityDescriptor(),
						entityReference.getSqlExpressionQualifier(),
						creationContext
				),
				entityReference.getNavigablePath(),
				creationContext
		);
	}

	@Override
	public void applyTableReferenceJoins(
			ColumnReferenceQualifier lhs,
			org.hibernate.sql.ast.JoinType joinType,
			SqlAliasBase sqlAliasBase,
			TableReferenceJoinCollector joinCollector,
			TableGroupContext tableGroupContext) {
		getEntityDescriptor().applyTableReferenceJoins( lhs, joinType, sqlAliasBase, joinCollector, tableGroupContext );
	}
}
