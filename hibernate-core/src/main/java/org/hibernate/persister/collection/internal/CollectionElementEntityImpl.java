/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.collection.internal;

import java.util.List;
import javax.persistence.metamodel.Type;

import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.mapping.Collection;
import org.hibernate.persister.collection.spi.AbstractCollectionElement;
import org.hibernate.persister.collection.spi.CollectionElementEntity;
import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.Navigable;
import org.hibernate.persister.common.spi.NavigableVisitationStrategy;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableSpace;
import org.hibernate.sql.ast.produce.spi.FromClauseIndex;
import org.hibernate.sql.ast.produce.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.produce.result.spi.Fetch;
import org.hibernate.sql.ast.produce.result.spi.FetchParent;
import org.hibernate.sql.ast.produce.result.spi.Return;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.type.spi.EntityType;

/**
 * @author Steve Ebersole
 */
public class CollectionElementEntityImpl<J>
		extends AbstractCollectionElement<J,EntityType<J>>
		implements CollectionElementEntity<J> {

	private final ElementClassification elementClassification;

	public CollectionElementEntityImpl(
			CollectionPersister persister,
			Collection mappingBinding,
			EntityType<J> ormType,
			ElementClassification elementClassification,
			List<Column> columns) {
		super( persister, ormType, columns );
		this.elementClassification = elementClassification;
	}

	@Override
	public EntityType<J> getOrmType() {
		return super.getOrmType();
	}

	@Override
	public EntityPersister<J> getEntityPersister() {
		return getOrmType().getEntityPersister();
	}

	@Override
	public Navigable findNavigable(String navigableName) {
		return getEntityPersister().findNavigable( navigableName );
	}

	@Override
	public String getEntityName() {
		return getEntityPersister().getEntityName();
	}

	@Override
	public String getJpaEntityName() {
		return getEntityPersister().getJpaEntityName();
	}

	@Override
	public ElementClassification getClassification() {
		return elementClassification;
	}

	@Override
	public Type.PersistenceType getPersistenceType() {
		return Type.PersistenceType.ENTITY;
	}

	@Override
	public void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitCollectionElementEntity( this );
	}

	@Override
	public TableGroup buildTableGroup(
			TableSpace tableSpace,
			SqlAliasBaseManager sqlAliasBaseManager,
			FromClauseIndex fromClauseIndex) {
		throw new NotYetImplementedException(  );
	}

	@Override
	public Return generateReturn(
			QueryResultCreationContext returnResolutionContext,
			TableGroup tableGroup) {
		throw new NotYetImplementedException(  );
	}

	@Override
	public Fetch generateFetch(
			QueryResultCreationContext returnResolutionContext,
			TableGroup tableGroup,
			FetchParent fetchParent) {
		throw new NotYetImplementedException(  );
	}
}
