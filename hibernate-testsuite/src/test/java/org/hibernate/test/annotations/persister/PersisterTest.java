package org.hibernate.test.annotations.persister;

import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.test.annotations.TestCase;

/**
 * @author Shawn Clowater
 */
public class PersisterTest extends TestCase {
	public PersisterTest(String x) {
		super( x );
	}

	public void testEntityEntityPersisterAndPersisterSpecified() throws Exception {
		//checks to see that the persister specified with the @Persister annotation takes precedence if a @Entity.persister() is also specified		
		PersistentClass persistentClass = (PersistentClass) getCfg().getClassMapping( Deck.class.getName() );
		assertEquals( "Incorrect Persister class for " + persistentClass.getMappedClass(), EntityPersister.class,
				persistentClass.getEntityPersisterClass() );
	}

	public void testEntityEntityPersisterSpecified() throws Exception {
		//tests the persister specified with an @Entity.persister()		
		PersistentClass persistentClass = (PersistentClass) getCfg().getClassMapping( Card.class.getName() );
		assertEquals( "Incorrect Persister class for " + persistentClass.getMappedClass(),
				SingleTableEntityPersister.class, persistentClass.getEntityPersisterClass() );
	}

	public void testCollectionPersisterSpecified() throws Exception {
		//tests the persister specified by the @Persister annotation on a collection
		Collection collection = (Collection) getCfg().getCollectionMapping( Deck.class.getName() + ".cards" );
		assertEquals( "Incorrect Persister class for collection " + collection.getRole(), CollectionPersister.class,
				collection.getCollectionPersisterClass() );
	}

	/**
	 * @see org.hibernate.test.annotations.TestCase#getAnnotatedClasses()
	 */
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Card.class,
				Deck.class
		};
	}

}