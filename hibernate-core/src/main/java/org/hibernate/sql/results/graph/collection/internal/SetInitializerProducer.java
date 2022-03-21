/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import org.hibernate.LockMode;
import org.hibernate.collection.spi.CollectionInitializerProducer;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;

/**
 * @author Steve Ebersole
 */
public class SetInitializerProducer implements CollectionInitializerProducer {
	private final PluralAttributeMapping setDescriptor;
	private final Fetch elementFetch;

	public SetInitializerProducer(
			PluralAttributeMapping setDescriptor,
			Fetch elementFetch) {
		this.setDescriptor = setDescriptor;
		this.elementFetch = elementFetch;
	}

	@Override
	public CollectionInitializer produceInitializer(
			NavigablePath navigablePath,
			PluralAttributeMapping attributeMapping,
			FetchParentAccess parentAccess,
			LockMode lockMode,
			DomainResultAssembler<?> collectionKeyAssembler,
			DomainResultAssembler<?> collectionValueKeyAssembler,
			AssemblerCreationState creationState) {
		final DomainResultAssembler<?> elementAssembler = elementFetch.createAssembler( parentAccess, creationState );

		return new SetInitializer(
				navigablePath,
				setDescriptor,
				parentAccess,
				lockMode,
				collectionKeyAssembler,
				collectionValueKeyAssembler,
				elementAssembler
		);
	}
}
