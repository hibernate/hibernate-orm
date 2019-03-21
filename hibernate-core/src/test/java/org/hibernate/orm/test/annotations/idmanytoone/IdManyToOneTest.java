/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.idmanytoone;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Emmanuel Bernard
 */
@Disabled("Composite non-aggregated Id not yet implemented")
public class IdManyToOneTest extends SessionFactoryBasedFunctionalTest {
	@Test
	public void testFkCreationOrdering() throws Exception {
		//no real test case, the sessionFactory building is tested
		inSession( session -> {

		} );
	}

	@Test
	public void testIdClassManyToOne() {
		inTransaction(
				sesison -> {
					Store store = new Store();
					Customer customer = new Customer();
					sesison.persist( store );
					sesison.persist( customer );
					StoreCustomer sc = new StoreCustomer( store, customer );
					sesison.persist( sc );
					sesison.flush();
					sesison.clear();

					store = sesison.get( Store.class, store.id );
					assertEquals( 1, store.customers.size() );
					assertEquals( customer.id, store.customers.iterator().next().customer.id );
				}

		);
		//TODO test Customers / ShoppingBaskets / BasketItems testIdClassManyToOneWithReferenceColumn
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7767")
	@Disabled("Criteria not yet implemented")
	public void testCriteriaRestrictionOnIdManyToOne() {
		inTransaction(
				session -> {
					session.createQuery( "from Course c join c.students cs join cs.student s where s.name = 'Foo'" )
							.list();

//					Criteria criteria = session.createCriteria( Course.class );
//					criteria.createCriteria( "students" ).createCriteria( "student" )
//							.add(
//									Restrictions.eq(
//											"name",
//											"Foo"
//									) );
//					criteria.list();
//
//					Criteria criteria2 = session.createCriteria( Course.class );
//					criteria2.createAlias( "students", "cs" );
//					criteria2.add( Restrictions.eq( "cs.value", "Bar" ) );
//					criteria2.createAlias( "cs.student", "s" );
//					criteria2.add( Restrictions.eq( "s.name", "Foo" ) );
//					criteria2.list();
				}
		);
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
