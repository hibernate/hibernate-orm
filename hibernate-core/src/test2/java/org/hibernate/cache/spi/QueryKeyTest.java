/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.spi;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.transform.AliasToBeanResultTransformer;
import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.hibernate.transform.AliasedTupleSubsetResultTransformer;
import org.hibernate.transform.CacheableResultTransformer;
import org.hibernate.transform.DistinctResultTransformer;
import org.hibernate.transform.DistinctRootEntityResultTransformer;
import org.hibernate.transform.PassThroughResultTransformer;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.transform.RootEntityResultTransformer;
import org.hibernate.transform.ToListResultTransformer;
import org.hibernate.transform.TupleSubsetResultTransformer;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

/**
 * Tests relating to {@link QueryKey} instances.
 *
 * @author Steve Ebersole
 */
public class QueryKeyTest extends BaseUnitTestCase {
	private static final String QUERY_STRING = "the query string";

	public static class AClass implements Serializable {
		private String propAccessedByField;
		private String propAccessedByMethod;
		private int propValue;

		public AClass() {
		}

		public AClass(String propAccessedByField) {
			this.propAccessedByField = propAccessedByField;
		}

		public String getPropAccessedByMethod() {
			return propAccessedByMethod;
		}

		public void setPropAccessedByMethod(String propAccessedByMethod) {
			this.propAccessedByMethod = propAccessedByMethod;
		}
	}

	@Test
	public void testSerializedEqualityResultTransformer() throws Exception {
		// settings are lazily initialized when calling transformTuple(),
		// so they have not been initialized for the following test
		// (it *should* be initialized before creating a QueryKey)
		doResultTransformerTest( new AliasToBeanResultTransformer( AClass.class ), false );

		// initialize settings for the next test
		AliasToBeanResultTransformer transformer = new AliasToBeanResultTransformer( AClass.class );
		transformer.transformTuple(
				new Object[] { "abc", "def" },
				new String[] { "propAccessedByField", "propAccessedByMethod" }
		);
		doResultTransformerTest( transformer, false );

		doResultTransformerTest( AliasToEntityMapResultTransformer.INSTANCE, true );
		doResultTransformerTest( DistinctResultTransformer.INSTANCE, true );
		doResultTransformerTest( DistinctRootEntityResultTransformer.INSTANCE, true );
		doResultTransformerTest( PassThroughResultTransformer.INSTANCE, true );
		doResultTransformerTest( RootEntityResultTransformer.INSTANCE, true );
		doResultTransformerTest( ToListResultTransformer.INSTANCE, true );
	}

	// Reproduces HHH-5628; commented out because FailureExpected is not working here...
	/*
	public void testAliasToBeanConstructorFailureExpected() throws Exception {
		// AliasToBeanConstructorResultTransformer is not Serializable because
		// java.lang.reflect.Constructor is not Serializable;
		doResultTransformerTest(
				new AliasToBeanConstructorResultTransformer( AClass.class.getConstructor( String.class ) ), false
		);
	}
	*/

	private void doResultTransformerTest(ResultTransformer transformer, boolean isSingleton) {
		Map transformerMap = new HashMap();

		transformerMap.put( transformer, "" );
		assert transformerMap.size() == 1 : "really messed up";
		Object old = transformerMap.put( transformer, "value" );
		assert old != null && transformerMap.size() == 1 : "apparent QueryKey equals/hashCode issue";

		// finally, lets serialize it and see what happens
		ResultTransformer transformer2 = ( ResultTransformer ) SerializationHelper.clone( transformer );
		old = transformerMap.put( transformer2, "new value" );
		assert old != null && transformerMap.size() == 1 : "deserialization did not set hashCode or equals properly";
		if ( isSingleton ) {
			assert transformer == transformer2: "deserialization issue for singleton transformer";
		}
		else {
			assert transformer != transformer2: "deserialization issue for non-singleton transformer";
		}
		assert transformer.equals( transformer2 ): "deep copy issue";
	}

	@Test
	public void testSerializedEquality() throws Exception {
		doTest( buildBasicKey( null ) );

		doTest( buildBasicKey( CacheableResultTransformer.create( null, null, new boolean[] { true } ) ) );
		doTest( buildBasicKey( CacheableResultTransformer.create( null, new String[] { null }, new boolean[] { true } ) ) );
		doTest( buildBasicKey( CacheableResultTransformer.create( null, new String[] { "a" }, new boolean[] { true } ) ) );
		doTest( buildBasicKey( CacheableResultTransformer.create( null, null, new boolean[] { false, true } ) ) );
		doTest( buildBasicKey( CacheableResultTransformer.create( null, new String[] { "a" }, new boolean[] { true, false } ) ) );
		doTest( buildBasicKey( CacheableResultTransformer.create( null, new String[] { "a", null }, new boolean[] { true, true } ) ) );
	}

	@Test
	public void testSerializedEqualityWithTupleSubsetResultTransfprmer() throws Exception {
		doTestWithTupleSubsetResultTransformer(
				new AliasToBeanResultTransformer( AClass.class ),
				new String[] { "propAccessedByField", "propAccessedByMethod" }
		);
		doTestWithTupleSubsetResultTransformer( AliasToEntityMapResultTransformer.INSTANCE, new String[] { "a", "b" } );
		doTestWithTupleSubsetResultTransformer( DistinctRootEntityResultTransformer.INSTANCE, new String[] { "a", "b" } );
		doTestWithTupleSubsetResultTransformer( PassThroughResultTransformer.INSTANCE, new String[] { "a", "b" } );
		doTestWithTupleSubsetResultTransformer( RootEntityResultTransformer.INSTANCE, new String[] { "a", "b" } );
		// The following are not TupleSubsetResultTransformers:
		// DistinctResultTransformer.INSTANCE
		// ToListResultTransformer.INSTANCE
	}

	public void doTestWithTupleSubsetResultTransformer(TupleSubsetResultTransformer transformer,
													   String[] aliases) throws Exception {
		doTest( buildBasicKey(
				CacheableResultTransformer.create(
						transformer,
						new String[] { aliases[ 0 ], aliases[ 1 ] },
						new boolean[] { true, true } )
		) );
		doTest( buildBasicKey(
				CacheableResultTransformer.create(
						transformer,
						new String[] { aliases[ 0 ], aliases[ 1 ] },
						new boolean[] { true, true, false } )
		) );
		doTest( buildBasicKey(
				CacheableResultTransformer.create(
						transformer,
						new String[] { aliases[ 1 ] },
						new boolean[] { true } )
		) );
		doTest( buildBasicKey(
				CacheableResultTransformer.create(
						transformer,
						new String[] { null, aliases[ 1 ] },
						new boolean[] { true, true } )
		) );
		doTest( buildBasicKey(
				CacheableResultTransformer.create(
						transformer,
						new String[] { aliases[ 0 ], null },
						new boolean[] { true, true } )
		) );
		doTest( buildBasicKey(
				CacheableResultTransformer.create(
						transformer,
						new String[] { aliases[ 0 ] },
						new boolean[] { false, true } )
		) );
		doTest( buildBasicKey(
				CacheableResultTransformer.create(
						transformer,
						new String[] { aliases[ 0 ] },
						new boolean[] { true, false } )
		) );
		doTest( buildBasicKey(
				CacheableResultTransformer.create(
						transformer,
						new String[] { aliases[ 0 ] },
						new boolean[] { false, true, false } )
		) );
		if ( ! ( transformer instanceof AliasedTupleSubsetResultTransformer ) ) {
			doTestWithTupleSubsetResultTransformerNullAliases( transformer );
		}
	}

	public void doTestWithTupleSubsetResultTransformerNullAliases(TupleSubsetResultTransformer transformer) throws Exception {
		doTest( buildBasicKey( CacheableResultTransformer.create( transformer, null, new boolean[] { true } ) ) );
		doTest( buildBasicKey( CacheableResultTransformer.create( transformer, null, new boolean[] { true, true } ) ) );
		doTest( buildBasicKey( CacheableResultTransformer.create( transformer, null, new boolean[] { true, true, true } ) ) );
		doTest( buildBasicKey( CacheableResultTransformer.create( transformer, null, new boolean[] { false, true } ) ) );
		doTest( buildBasicKey( CacheableResultTransformer.create( transformer, null, new boolean[] { true, false } ) ) );
		doTest( buildBasicKey( CacheableResultTransformer.create( transformer, null, new boolean[] { false, true, true } ) ) );
		doTest( buildBasicKey( CacheableResultTransformer.create( transformer, null, new boolean[] {true, false, true } ) ) );
		doTest( buildBasicKey( CacheableResultTransformer.create( transformer, null, new boolean[] {true, true, false } ) ) );
		doTest( buildBasicKey( CacheableResultTransformer.create( transformer, null, new boolean[] {false, false, true } ) ) );
		doTest( buildBasicKey( CacheableResultTransformer.create( transformer, null, new boolean[] {false, true, false } ) ) );
		doTest( buildBasicKey( CacheableResultTransformer.create( transformer, null, new boolean[] {false, false, true } ) ) );
	}

	private QueryKey buildBasicKey(CacheableResultTransformer resultTransformer) {
		return new QueryKey(
				QUERY_STRING,
				ArrayHelper.EMPTY_TYPE_ARRAY, 		// positional param types
				ArrayHelper.EMPTY_OBJECT_ARRAY,		// positional param values
				Collections.EMPTY_MAP,				// named params
				null,								// firstRow selection
				null,								// maxRows selection
				Collections.EMPTY_SET, 				// filter keys
				null,								// tenantIdentifier
				resultTransformer					// the result transformer
		);
	}

	private void doTest(QueryKey key) {
		Map keyMap = new HashMap();
		Map transformerMap = new HashMap();

		keyMap.put( key, "" );
		assert keyMap.size() == 1 : "really messed up";
		Object old = keyMap.put( key, "value" );
		assert old != null && keyMap.size() == 1 : "apparent QueryKey equals/hashCode issue";

		if ( key.getResultTransformer() != null ) {
			transformerMap.put( key.getResultTransformer(), "" );
			assert transformerMap.size() == 1 : "really messed up";
			old = transformerMap.put( key.getResultTransformer(), "value" );
			assert old != null && transformerMap.size() == 1 : "apparent QueryKey equals/hashCode issue";
		}

		// finally, lets serialize it and see what happens
		QueryKey key2 = ( QueryKey ) SerializationHelper.clone( key );
		assert key != key2 : "deep copy issue";
		old = keyMap.put( key2, "new value" );
		assert old != null && keyMap.size() == 1 : "deserialization did not set hashCode or equals properly";
		if ( key.getResultTransformer() == null ) {
			assert key2.getResultTransformer() == null;
		}
		else {
			old = transformerMap.put( key2.getResultTransformer(), "new value" );
			assert old != null && transformerMap.size() == 1 : "deserialization did not set hashCode or equals properly";
				assert key.getResultTransformer() != key2.getResultTransformer(): "deserialization issue for non-singleton transformer";
				assert key.getResultTransformer().equals( key2.getResultTransformer() ): "deep copy issue";
		}
	}
}
