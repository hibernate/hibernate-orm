/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.test.annotations.idmanytoone;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Emmanuel Bernard
 */
@FailureExpectedWithNewMetamodel(
		message = "The problem here is actually the combination of @ManyToOne and @Basic on attributes; need to determine if that's really valid"
)
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
}
