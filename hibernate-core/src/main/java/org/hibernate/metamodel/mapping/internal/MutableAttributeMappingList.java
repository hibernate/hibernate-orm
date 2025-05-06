/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.AttributeMappingsList;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.sql.results.graph.Fetchable;

/**
 * This mutable representation of AttributeMappingsList is meant to
 * exist temporarily to assist migration to the new contract.
 * @deprecated  Please get rid of it: such collections should be immutable.
 */
@Deprecated
public final class MutableAttributeMappingList implements AttributeMappingsList {

	private final List list;

	public MutableAttributeMappingList(final int length) {
		this.list = new ArrayList<>( length );
	}

	@Override
	public int size() {
		return list.size();
	}

	@Override
	public AttributeMapping get(int idx) {
		return asAttributeMapping( this.list.get( idx ) );
	}

	@Override
	public void forEach(final Consumer<? super AttributeMapping> consumer) {
		for ( int i = 0; i < list.size(); i++ ) {
			consumer.accept( asAttributeMapping( list.get( i ) ) );
		}
	}

	@Override
	public void indexedForEach(final IndexedConsumer<? super AttributeMapping> consumer) {
		for ( int i = 0; i < list.size(); i++ ) {
			consumer.accept( i, get( i ) );
		}
	}

	public void clear() {
		this.list.clear();
	}

	public void add(final AttributeMapping attributeMapping) {
		this.list.add( attributeMapping );
	}

	/**
	 * @deprecated should be removed
	 */
	@Deprecated
	public SingularAttributeMapping getSingularAttributeMapping(int idx) {
		//TBD get rid of this cast - but it's low priority: apparently
		//not used much at all after bootstrap.
		return (SingularAttributeMapping) this.list.get( idx );
	}

	public void setAttributeMapping(int i, AttributeMapping attributeMapping) {
		this.list.set( i, attributeMapping );
	}

	private static AttributeMapping asAttributeMapping(final Object object) {
		//Check for BasicAttributeMapping as it's not an interface and has
		//a very high likelihood to match; this helps to mitigate JDK-8180450.
		if ( object instanceof BasicAttributeMapping basicAttributeMapping ) {
			return basicAttributeMapping;
		}
		else if ( object instanceof Fetchable fetchable ) {
			//Alternatively, cast to Fetchable for consistency with most other code:
			//again this is a likelihood game to mitigate for JDK-8180450;
			//For the longer term we hope that either JDK-8180450 gets fixed
			//or this implementation can be deleted (using exclusively the
			//immutable versions of AttributeMappingsList, which isn't affected by this issue);
			//ideally both the JDK issue gets fixed and this class gets removed.
			return fetchable.asAttributeMapping();
		}
		else {
			throw new IllegalArgumentException( "Unexpected attribute mapping" );
		}
	}

}
