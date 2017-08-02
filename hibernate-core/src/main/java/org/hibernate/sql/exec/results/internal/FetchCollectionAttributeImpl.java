/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.results.internal;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.exec.results.spi.FetchCollectionAttribute;
import org.hibernate.sql.exec.results.spi.FetchParent;
import org.hibernate.sql.exec.results.spi.FetchParentAccess;
import org.hibernate.sql.exec.results.spi.InitializerCollector;
import org.hibernate.sql.exec.results.spi.QueryResultCreationContext;

/**
 * @author Steve Ebersole
 */
public class FetchCollectionAttributeImpl extends AbstractCollectionReference implements FetchCollectionAttribute {
	private final FetchParent fetchParent;
	private final FetchStrategy fetchStrategy;
	private final PluralPersistentAttribute pluralAttribute;
	private final QueryResultCreationContext creationContext;

	public FetchCollectionAttributeImpl(
			FetchParent fetchParent,
			PluralPersistentAttribute pluralAttribute,
			FetchStrategy fetchStrategy,
			String resultVariable,
			QueryResultCreationContext creationContext) {
		super( pluralAttribute, resultVariable );
		this.fetchParent = fetchParent;
		this.pluralAttribute = pluralAttribute;
		this.fetchStrategy = fetchStrategy;
		this.creationContext = creationContext;
	}

	@Override
	public void registerInitializers(FetchParentAccess parentAccess, InitializerCollector collector) {
		throw new NotYetImplementedException(  );
	}

	@Override
	public PluralPersistentAttribute getFetchedNavigable() {
		return getNavigable();
	}

	@Override
	public FetchParent getFetchParent() {
		return fetchParent;
	}

	@Override
	public FetchStrategy getFetchStrategy() {
		return fetchStrategy;
	}

	@Override
	public boolean isNullable() {
		return true;
	}
}
