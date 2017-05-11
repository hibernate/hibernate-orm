/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.collection.internal;

import java.util.List;

import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.mapping.Collection;
import org.hibernate.persister.collection.spi.AbstractCollectionElement;
import org.hibernate.persister.collection.spi.CollectionElementEmbedded;
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
import org.hibernate.sql.ast.produce.result.spi.QueryResult;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.type.spi.EmbeddedType;

/**
 * @author Steve Ebersole
 */
public class CollectionElementEmbeddedImpl<J>
		extends AbstractCollectionElement<J,EmbeddedType<J>>
		implements CollectionElementEmbedded<J> {
	public CollectionElementEmbeddedImpl(
			CollectionPersister persister,
			Collection mappingBinding,
			EmbeddedType<J> ormType,
			List<Column> columns) {
		super( persister, ormType, columns );
	}

	@Override
	public EmbeddedType<J> getOrmType() {
		return super.getOrmType();
	}

	@Override
	public Navigable findNavigable(String navigableName) {
		return getEmbeddablePersister().findNavigable( navigableName );
	}

	@Override
	public EmbeddedPersister getEmbeddablePersister() {
		return getOrmType().getEmbeddablePersister();
	}

	@Override
	public void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitCollectionElementEmbedded( this );
	}

	@Override
	public TableGroup buildTableGroup(
			TableSpace tableSpace,
			SqlAliasBaseManager sqlAliasBaseManager,
			FromClauseIndex fromClauseIndex) {
		throw new NotYetImplementedException(  );
	}

	@Override
	public QueryResult generateReturn(
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
