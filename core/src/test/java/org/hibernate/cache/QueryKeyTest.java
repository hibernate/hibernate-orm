/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.cache;

import java.util.Collections;
import java.util.HashMap;

import junit.framework.TestCase;

import org.hibernate.engine.QueryParameters;
import org.hibernate.EntityMode;
import org.hibernate.transform.RootEntityResultTransformer;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.transform.DistinctRootEntityResultTransformer;
import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.hibernate.transform.PassThroughResultTransformer;
import org.hibernate.transform.DistinctResultTransformer;
import org.hibernate.util.SerializationHelper;
import org.hibernate.util.ArrayHelper;

/**
 * Tests relating to {@link QueryKey} instances.
 *
 * @author Steve Ebersole
 */
public class QueryKeyTest extends TestCase {
	private static final String QUERY_STRING = "the query string";

	public void testSerializedEquality() {
		doTest( buildBasicKey( new QueryParameters() ) );
	}

	public void testSerializedEqualityWithResultTransformer() {
		doTest( buildBasicKey( buildQueryParameters( RootEntityResultTransformer.INSTANCE ) ) );
		doTest( buildBasicKey( buildQueryParameters( DistinctRootEntityResultTransformer.INSTANCE ) ) );
		doTest( buildBasicKey( buildQueryParameters( DistinctResultTransformer.INSTANCE ) ) );
		doTest( buildBasicKey( buildQueryParameters( AliasToEntityMapResultTransformer.INSTANCE ) ) );
		doTest( buildBasicKey( buildQueryParameters( PassThroughResultTransformer.INSTANCE ) ) );
	}

	private QueryParameters buildQueryParameters(ResultTransformer resultTransformer) {
		return new QueryParameters(
				ArrayHelper.EMPTY_TYPE_ARRAY, 		// param types
				ArrayHelper.EMPTY_OBJECT_ARRAY,		// param values
				Collections.EMPTY_MAP,				// lock modes
				null,								// row selection
				false,								// cacheable?
				"",									// cache region
				"", 								// SQL comment
				false,								// is natural key lookup?
				resultTransformer					// the result transformer, duh! ;)
		);
	}

	private QueryKey buildBasicKey(QueryParameters queryParameters) {
		return new QueryKey( QUERY_STRING, queryParameters, Collections.EMPTY_SET, EntityMode.POJO );
	}

	private void doTest(QueryKey key) {
		HashMap map = new HashMap();

		map.put( key, "" );
		assert map.size() == 1 : "really messed up";

		Object old = map.put( key, "value" );
		assert old != null && map.size() == 1 : "apparent QueryKey equals/hashCode issue";

		// finally, lets serialize it and see what happens
		QueryKey key2 = ( QueryKey ) SerializationHelper.clone( key );
		assert key != key2 : "deep copy issue";
		old = map.put( key2, "new value" );
		assert old != null && map.size() == 1 : "deserialization did not set hashCode or equals properly";
	}
}
