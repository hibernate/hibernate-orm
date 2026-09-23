/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.MappingException;

/// Ordered archive data for selectable ordering, without serialized mutable hash keys.
///
/// @author Steve Ebersole
final class SelectableOrderings<S extends Selectable> implements Serializable {
	private final List<Entry<S>> entries = new ArrayList<>();

	void put(S selectable, String order) {
		for ( int i = 0; i < entries.size(); i++ ) {
			if ( entries.get( i ).selectable.equals( selectable ) ) {
				entries.set( i, new Entry<>( selectable, order ) );
				return;
			}
		}
		entries.add( new Entry<>( selectable, order ) );
	}

	Map<S, String> asMap() {
		final Map<S, String> result = new LinkedHashMap<>();
		for ( var entry : entries ) {
			if ( result.containsKey( entry.selectable ) ) {
				throw new MappingException( "Physical name collision in selectable ordering: " + entry.selectable );
			}
			result.put( entry.selectable, entry.order );
		}
		return java.util.Collections.unmodifiableMap( result );
	}

	void visit(java.util.function.Consumer<Selectable> consumer) {
		entries.forEach( entry -> consumer.accept( entry.selectable ) );
	}

	/// An ordering entry preserves the original selectable reference in the archive.
	///
	/// @author Steve Ebersole
	private record Entry<S extends Selectable>(S selectable, String order) implements Serializable {}
}
