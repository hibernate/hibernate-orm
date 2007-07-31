package org.hibernate.test.collection;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.test.collection.backref.map.compkey.BackrefCompositeMapKeyTest;
import org.hibernate.test.collection.bag.PersistentBagTest;
import org.hibernate.test.collection.idbag.PersistentIdBagTest;
import org.hibernate.test.collection.list.PersistentListTest;
import org.hibernate.test.collection.map.PersistentMapTest;
import org.hibernate.test.collection.original.CollectionTest;
import org.hibernate.test.collection.set.PersistentSetTest;

/**
 * Suite of collection (i.e. PersistentCollection) related tests
 *
 * @author Steve Ebersole
 */
public class CollectionSuite {

	public static Test suite() {
		TestSuite suite = new TestSuite( "Collection-related tests" );
		suite.addTest( PersistentBagTest.suite() );
		suite.addTest( PersistentIdBagTest.suite() );
		suite.addTest( PersistentListTest.suite() );
		suite.addTest( PersistentMapTest.suite() );
		suite.addTest( CollectionTest.suite() );
		suite.addTest( PersistentSetTest.suite() );
		suite.addTest( BackrefCompositeMapKeyTest.suite() );
		return suite;
	}

}
