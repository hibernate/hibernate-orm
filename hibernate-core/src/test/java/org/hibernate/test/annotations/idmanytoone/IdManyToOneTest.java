//$Id$
package org.hibernate.test.annotations.idmanytoone;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.annotations.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class IdManyToOneTest extends TestCase {
	public void testFkCreationOrdering() throws Exception {
		//no real test case, the sessionFactory building is tested
		Session s = openSession();
		s.close();
	}

	public void getBiDirOneToManyInId() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		CardKey key = new CardKey();
		s.persist( key );
		Project project = new Project();
		s.persist( project );
		Card card = new Card();
		card.getPrimaryKey().setProject( project );
		s.persist( card );
		CardField field = new CardField();
		field.getPrimaryKey().setKey( key );
		field.getPrimaryKey().setCard( card );
		s.persist( field );
		card.setMainCardField( field );
		s.flush();
		s.clear();
		card = (Card) s.createQuery( "from Card c").list().get(0);
		assertEquals( 1, card.getFields().size() );
		assertEquals( card.getMainCardField(), card.getFields().iterator().next() );
		tx.rollback();
		s.close();
	}

	public void testIdClassManyToOne() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Store store = new Store();
		Customer customer = new Customer();
		s.persist( store );
		s.persist( customer );
		StoreCustomer sc = new StoreCustomer( store, customer );
		s.persist( sc );
		s.flush();
		s.clear();

		store = (Store) s.get(Store.class, store.id );
		assertEquals( 1, store.customers.size() );
		assertEquals( customer.id, store.customers.iterator().next().customer.id );
		tx.rollback();

		//TODO test Customers / ShoppingBaskets / BasketItems testIdClassManyToOneWithReferenceColumn
		s.close();
	}

	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Store.class,
				Customer.class,
				StoreCustomer.class,
				CardKey.class,
				CardField.class,
				Card.class,
				Project.class,

				//tested only through deployment
				//ANN-590 testIdClassManyToOneWithReferenceColumn 
				Customers.class,
				ShoppingBaskets.class,
				ShoppingBasketsPK.class,
				BasketItems.class,
				BasketItemsPK.class
		};
	}
}
