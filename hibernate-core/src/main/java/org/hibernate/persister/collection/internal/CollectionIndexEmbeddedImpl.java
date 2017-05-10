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
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.persister.collection.spi.AbstractCollectionIndex;
import org.hibernate.persister.collection.spi.CollectionIndexEmbedded;
import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.Navigable;
import org.hibernate.persister.common.spi.NavigableVisitationStrategy;
import org.hibernate.persister.embedded.spi.EmbeddedPersister;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableSpace;
import org.hibernate.sql.ast.produce.spi.FromClauseIndex;
import org.hibernate.sql.ast.produce.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.produce.result.spi.Fetch;
import org.hibernate.sql.ast.produce.result.spi.FetchParent;
import org.hibernate.sql.ast.produce.result.spi.Return;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.type.spi.EmbeddedType;

/**
 * @author Steve Ebersole
 */
public class CollectionIndexEmbeddedImpl<J>
		extends AbstractCollectionIndex<J,EmbeddedType<J>>
		implements CollectionIndexEmbedded<J> {
	public CollectionIndexEmbeddedImpl(
			CollectionPersister persister,
			IndexedCollection mappingBInding,
			EmbeddedType<J> ormType,
			List<Column> columns) {
		super( persister, ormType, columns );
	}

	@Override
	@SuppressWarnings("unchecked")
	public EmbeddedType<J> getOrmType() {
		return super.getOrmType();
	}

	@Override
	public EmbeddedPersister getEmbeddablePersister() {
		return getOrmType().getEmbeddablePersister();
	}

	@Override
	public Type.PersistenceType getPersistenceType() {
		return Type.PersistenceType.EMBEDDABLE;
	}

	@Override
	public Navigable findNavigable(String navigableName) {
		return getEmbeddablePersister().findNavigable( navigableName );
	}

	@Override
	public IndexClassification getClassification() {
		return IndexClassification.EMBEDDABLE;
	}

	@Override
	public void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitCollectionIndexEmbedded( this );
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
