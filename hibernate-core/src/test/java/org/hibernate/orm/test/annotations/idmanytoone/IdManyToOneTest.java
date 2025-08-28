/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.idmanytoone;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;

import org.hibernate.Session;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Emmanuel Bernard
 */
public class IdManyToOneTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testFkCreationOrdering() {
		//no real test case, the sessionFactory building is tested
		Session s = openSession();
		s.close();
	}

	@Test
	public void testIdClassManyToOne() {
		inTransaction( s-> {
			Store store = new Store();
			Customer customer = new Customer();
			s.persist( store );
			s.persist( customer );
			StoreCustomer sc = new StoreCustomer( store, customer );
			s.persist( sc );
			s.flush();
			s.clear();

			store = s.get(Store.class, store.id );
			assertEquals( 1, store.customers.size() );
			assertEquals( customer.id, store.customers.iterator().next().customer.id );
		} );
		//TODO test Customers / ShoppingBaskets / BasketItems testIdClassManyToOneWithReferenceColumn
	}

	@Test
	@JiraKey( value = "HHH-7767" )
	public void testCriteriaRestrictionOnIdManyToOne() {
		inTransaction( s -> {
			s.createQuery( "from Course c join c.students cs join cs.student s where s.name = 'Foo'", Object[].class ).list();

			CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
			CriteriaQuery<Course> criteria = criteriaBuilder.createQuery( Course.class );
			Root<Course> root = criteria.from( Course.class );
			Join<Object, Object> students = root.join( "students", JoinType.INNER );
			Join<Object, Object> student = students.join( "student", JoinType.INNER );
			criteria.where( criteriaBuilder.equal( student.get( "name" ), "Foo" ) );
			s.createQuery( criteria ).list();
//		Criteria criteria = s.createCriteria( Course.class );
//        criteria.createCriteria( "students" ).createCriteria( "student" ).add( Restrictions.eq( "name", "Foo" ) );
//        criteria.list();

//		CriteriaQuery<Course> criteria2 = criteriaBuilder.createQuery( Course.class );

//        Criteria criteria2 = s.createCriteria( Course.class );
//        criteria2.createAlias( "students", "cs" );
//        criteria2.add( Restrictions.eq( "cs.value", "Bar" ) );
//        criteria2.createAlias( "cs.student", "s" );
//        criteria2.add( Restrictions.eq( "s.name", "Foo" ) );
//        criteria2.list();
		} );
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
