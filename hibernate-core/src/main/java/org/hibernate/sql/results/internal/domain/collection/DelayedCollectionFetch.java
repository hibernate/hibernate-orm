/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.collection;

import java.util.function.Consumer;

import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.sql.results.spi.AssemblerCreationContext;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.CollectionFetch;
import org.hibernate.sql.results.spi.CollectionInitializer;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.Initializer;

/**
 * @author Steve Ebersole
 */
public class DelayedCollectionFetch extends AbstractCollectionMappingNode implements CollectionFetch {
	public DelayedCollectionFetch(
			FetchParent fetchParent,
			PluralPersistentAttribute pluralAttribute,
			String resultVariable,
			DomainResult collectionKeyResult) {
		super(
				fetchParent,
				pluralAttribute,
				resultVariable,
				collectionKeyResult,
				null
		);
	}

	@Override
	public FetchParent getFetchParent() {
		return super.getFetchParent();
	}

	@Override
	public PluralPersistentAttribute getFetchedNavigable() {
		return getPluralAttribute();
	}

	@Override
	public boolean isNullable() {
		// todo (6.0) : could depend on an explicit join type (in HQL e.g.)
		return getPluralAttribute().isNullable();
	}

	@Override
	public DomainResultAssembler createAssembler(
			FetchParentAccess parentAccess,
			Consumer<Initializer> collector,
			AssemblerCreationContext creationContext,
			AssemblerCreationState creationState) {
		final DomainResultAssembler keyAssembler = getKeyContainerResult().createResultAssembler(
				collector,
				creationState,
				creationContext
		);
		final CollectionInitializer collectionInitializer = new DelayedCollectionInitializer(
				parentAccess,
				getNavigablePath(),
				getCollectionDescriptor(),
				keyAssembler,
				keyAssembler
		);

		collector.accept( collectionInitializer );

		return new PluralAttributeAssemblerImpl( collectionInitializer );
	}
}
