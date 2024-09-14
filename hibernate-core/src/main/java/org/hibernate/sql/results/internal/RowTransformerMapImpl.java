/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.results.internal;

import jakarta.persistence.TupleElement;
import org.hibernate.sql.results.spi.RowTransformer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link RowTransformer} instantiating a {@link Map}
 *
 * @author Gavin King
 */
public class RowTransformerMapImpl implements RowTransformer<Map<String,Object>> {
	private final TupleMetadata tupleMetadata;

	public RowTransformerMapImpl(TupleMetadata tupleMetadata) {
		this.tupleMetadata = tupleMetadata;
	}

	@Override
	public Map<String,Object> transformRow(Object[] row) {
		Map<String,Object> map = new HashMap<>( row.length );
		List<TupleElement<?>> list = tupleMetadata.getList();
		for ( int i = 0; i < row.length; i++ ) {
			String alias = list.get(i).getAlias();
			if ( alias == null ) {
				alias = Integer.toString(i);
			}
			map.put( alias, row[i] );
		}
		return map;
	}

	@Override
	public int determineNumberOfResultElements(int rawElementCount) {
		return 1;
	}
}
