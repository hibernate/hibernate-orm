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
		doTest( buildBasicKey( null ) );
	}

	public void testSerializedEqualityWithResultTransformer() {
		doTest( buildBasicKey( RootEntityResultTransformer.INSTANCE ) );
		doTest( buildBasicKey( DistinctRootEntityResultTransformer.INSTANCE ) );
		doTest( buildBasicKey( DistinctResultTransformer.INSTANCE ) );
		doTest( buildBasicKey( AliasToEntityMapResultTransformer.INSTANCE ) );
		doTest( buildBasicKey( PassThroughResultTransformer.INSTANCE ) );
	}

	private QueryKey buildBasicKey(ResultTransformer resultTransformer) {
		return new QueryKey(
				QUERY_STRING,
				ArrayHelper.EMPTY_TYPE_ARRAY, 		// positional param types
				ArrayHelper.EMPTY_OBJECT_ARRAY,		// positional param values
				Collections.EMPTY_MAP,				// named params
				null,								// firstRow selection
				null,								// maxRows selection
				Collections.EMPTY_SET, 				// filter keys
				EntityMode.POJO,					// entity mode
				resultTransformer					// the result transformer
		);
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
