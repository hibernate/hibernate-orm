/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.results.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.persistence.TupleElement;

/**
 * Metadata about the tuple structure.
 *
 * @author Christian Beikov
 */
public final class TupleMetadata {
	private final Map<TupleElement<?>, Integer> index;
	private Map<String, Integer> nameIndex;
	private List<TupleElement<?>> list;

	public TupleMetadata(Map<TupleElement<?>, Integer> index) {
		this.index = index;
	}

	public Integer get(TupleElement<?> tupleElement) {
		return index.get( tupleElement );
	}

	public Integer get(String name) {
		Map<String, Integer> nameIndex = this.nameIndex;
		if ( nameIndex == null ) {
			nameIndex = new HashMap<>( index.size() );
			for ( Map.Entry<TupleElement<?>, Integer> entry : index.entrySet() ) {
				nameIndex.put( entry.getKey().getAlias(), entry.getValue() );
			}
			this.nameIndex = nameIndex = Collections.unmodifiableMap( nameIndex );
		}
		return nameIndex.get( name );
	}

	public List<TupleElement<?>> getList() {
		List<TupleElement<?>> list = this.list;
		if ( list == null ) {
			final TupleElement<?>[] array = new TupleElement[index.size()];
			for ( Map.Entry<TupleElement<?>, Integer> entry : index.entrySet() ) {
				array[entry.getValue()] = entry.getKey();
			}
			this.list = list = Collections.unmodifiableList( Arrays.asList( array ) );
		}
		return list;
	}
}
