/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.produce.spi.SqlExpressionQualifier;
import org.hibernate.sql.results.spi.EntityFetch;
import org.hibernate.sql.results.spi.EntitySqlSelectionMappings;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.InitializerCollector;
import org.hibernate.sql.results.spi.QueryResultCreationContext;

/**
 * @author Steve Ebersole
 */
public class EntityFetchImpl extends AbstractFetchParent implements EntityFetch {
	private final FetchParent fetchParent;
	private final SqlExpressionQualifier qualifier;
	private final FetchStrategy fetchStrategy;

	private final EntitySqlSelectionMappings sqlSelectionMappings;

	public EntityFetchImpl(
			FetchParent fetchParent,
			SqlExpressionQualifier qualifier,
			EntityValuedNavigable fetchedNavigable,
			NavigablePath navigablePath,
			FetchStrategy fetchStrategy,
			QueryResultCreationContext creationContext) {
		super( fetchedNavigable, navigablePath );
		this.fetchParent = fetchParent;
		this.qualifier = qualifier;
		this.sqlSelectionMappings = EntitySqlSelectionMappingsBuilder.buildSqlSelectionMappings(
				getEntityDescriptor(),
				qualifier,
				creationContext
		);
		this.fetchStrategy = fetchStrategy;
	}

	@Override
	public FetchParent getFetchParent() {
		return fetchParent;
	}

	@Override
	public SqlExpressionQualifier getSqlExpressionQualifier() {
		return qualifier;
	}

	@Override
	public EntityValuedNavigable getFetchedNavigable() {
		return getFetchContainer();
	}

	@Override
	public FetchStrategy getFetchStrategy() {
		return fetchStrategy;
	}

	@Override
	public boolean isNullable() {
		throw new NotYetImplementedException(  );
	}

	@Override
	public EntityValuedNavigable getFetchContainer() {
		return (EntityValuedNavigable) super.getFetchContainer();
	}

	@Override
	public EntityDescriptor getEntityDescriptor() {
		return getFetchContainer().getEntityDescriptor();
	}

	@Override
	public void registerInitializers(FetchParentAccess parentAccess, InitializerCollector collector) {
		final EntityFetchInitializer initializer = new EntityFetchInitializer(
				parentAccess,
				this,
				sqlSelectionMappings,
				false
		);

		collector.addInitializer( initializer );
		registerFetchInitializers( initializer, collector );
	}
}
