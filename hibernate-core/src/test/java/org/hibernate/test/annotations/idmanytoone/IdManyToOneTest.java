/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.idmanytoone;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Restrictions;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Emmanuel Bernard
 */
public class IdManyToOneTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testFkCreationOrdering() throws Exception {
		//no real test case, the sessionFactory building is tested
		Session s = openSession();
		s.close();
	}

	@Test
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

    @Test
	@TestForIssue( jiraKey = "HHH-7767" )
    public void testCriteriaRestrictionOnIdManyToOne() {
        Session s = openSession();
        s.beginTransaction();

        s.createQuery( "from Course c join c.students cs join cs.student s where s.name = 'Foo'" ).list();

        Criteria criteria = s.createCriteria( Course.class );
        criteria.createCriteria( "students" ).createCriteria( "student" ).add( Restrictions.eq( "name", "Foo" ) );
        criteria.list();

        Criteria criteria2 = s.createCriteria( Course.class );
        criteria2.createAlias( "students", "cs" );
        criteria2.add( Restrictions.eq( "cs.value", "Bar" ) );
        criteria2.createAlias( "cs.student", "s" );
        criteria2.add( Restrictions.eq( "s.name", "Foo" ) );
        criteria2.list();

        s.getTransaction().commit();
        s.close();
    }

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Store.class,
				Customer.class,
				StoreCustomer.class,
				CardKey.class,
				CardField.class,
				Card.class,
				Project.class,
                Course.class,
                Student.class,
                CourseStudent.class,

				//tested only through deployment
				//ANN-590 testIdClassManyToOneWithReferenceColumn 
				Customers.class,
				ShoppingBaskets.class,
				ShoppingBasketsPK.class,
				BasketItems.class,
				BasketItemsPK.class
		};
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.setImplicitNamingStrategy( ImplicitNamingStrategyJpaCompliantImpl.INSTANCE );
	}
}
