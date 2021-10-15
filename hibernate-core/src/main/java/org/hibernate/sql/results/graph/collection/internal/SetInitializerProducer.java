/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import org.hibernate.LockMode;
import org.hibernate.collection.spi.CollectionInitializerProducer;
import org.hibernate.engine.FetchStyle;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;
import org.hibernate.sql.results.graph.collection.CollectionLoadingLogger;

import static org.hibernate.sql.results.graph.collection.CollectionLoadingLogger.COLL_LOAD_LOGGER;

/**
 * @author Steve Ebersole
 */
public class SetInitializerProducer implements CollectionInitializerProducer {
	private final PluralAttributeMapping setDescriptor;
	private final Fetch elementFetch;

	private final boolean isSubSelectFetchable;

	public SetInitializerProducer(
			PluralAttributeMapping setDescriptor,
			Fetch elementFetch) {
		this.setDescriptor = setDescriptor;
		this.elementFetch = elementFetch;

		isSubSelectFetchable = setDescriptor.getMappedFetchOptions().getStyle() == FetchStyle.SUBSELECT;
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

		if ( isSubSelectFetchable && navigablePath.isRoot() ) {
			COLL_LOAD_LOGGER.debugf( "Creating sub-select-fetch initializer : `%`", navigablePath );
		}

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
