/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria;

import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.hibernate.cfg.JpaComplianceSettings;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.orm.test.jpa.metamodel.Address;
import org.hibernate.orm.test.jpa.metamodel.Alias;
import org.hibernate.orm.test.jpa.metamodel.CreditCard;
import org.hibernate.orm.test.jpa.metamodel.Customer;
import org.hibernate.orm.test.jpa.metamodel.Customer_;
import org.hibernate.orm.test.jpa.metamodel.Info;
import org.hibernate.orm.test.jpa.metamodel.LineItem;
import org.hibernate.orm.test.jpa.metamodel.Order;
import org.hibernate.orm.test.jpa.metamodel.Phone;
import org.hibernate.orm.test.jpa.metamodel.Product;
import org.hibernate.orm.test.jpa.metamodel.Spouse;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@Jpa(
		annotatedClasses = {Customer.class, Address.class, Alias.class, CreditCard.class, Order.class, LineItem.class,
				Product.class, Spouse.class, Info.class, Phone.class},
		integrationSettings = {@Setting(name = JpaComplianceSettings.JPA_QUERY_COMPLIANCE, value = "true")}
)
public class ManipulationCriteriaTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void basicTest(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {

			CriteriaBuilder builder = entityManager.getCriteriaBuilder();

			{
				CriteriaDelete<Customer> deleteCriteria = builder.createCriteriaDelete( Customer.class );
				deleteCriteria.from( Customer.class );
				entityManager.createQuery( deleteCriteria ).executeUpdate();
			}

			{
				CriteriaDelete<Customer> deleteCriteria = builder.createCriteriaDelete( Customer.class );
				Root<Customer> root = deleteCriteria.from( Customer.class );
				deleteCriteria.where(
						builder.equal(
								root.get( Customer_.name ),
								"Acme"
						)
				);
				entityManager.createQuery( deleteCriteria ).executeUpdate();
			}

			{
				CriteriaUpdate<Customer> updateCriteria = builder.createCriteriaUpdate( Customer.class );
				updateCriteria.from( Customer.class );
				updateCriteria.set( Customer_.name, "Acme" );
				entityManager.createQuery( updateCriteria ).executeUpdate();
			}

			{
				CriteriaUpdate<Customer> updateCriteria = builder.createCriteriaUpdate( Customer.class );
				Root<Customer> root = updateCriteria.from( Customer.class );
				updateCriteria.set( Customer_.age, 23 );
				updateCriteria.where(
						builder.equal(
								root.get( Customer_.name ),
								"Acme"
						)
				);
				entityManager.createQuery( updateCriteria ).executeUpdate();
			}

			{
				CriteriaDelete<Customer> deleteCriteria = builder.createCriteriaDelete( Customer.class );
				Root<Customer> root = deleteCriteria.from( Customer.class );
				deleteCriteria.where(
						builder.equal(
								root.get( Customer_.name ),
								"Acme"
						),
						builder.equal(
								root.get( Customer_.age ),
								23
						)
				);
				entityManager.createQuery( deleteCriteria ).executeUpdate();
			}

		} );
	}

	@Test
	public void testNoAssignments(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {

			CriteriaBuilder builder = entityManager.getCriteriaBuilder();

			assertThrows(
					IllegalArgumentException.class,
					() -> {
						CriteriaUpdate<Customer> updateCriteria = builder.createCriteriaUpdate( Customer.class );
						updateCriteria.from( Customer.class );
						entityManager.createQuery( updateCriteria ).executeUpdate();
					},
					"Expecting failure due to no assignments"
			);

			// changed to rollback since HHH-8442 causes transaction to be marked for rollback only
			assertTrue( entityManager.getTransaction().getRollbackOnly() );
		} );
}

	@Test
	@JiraKey(value = "HHH-8434")
	public void basicMultipleAssignments(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {

			CriteriaBuilder builder = entityManager.getCriteriaBuilder();
			CriteriaUpdate<Customer> updateCriteria = builder.createCriteriaUpdate( Customer.class );
			updateCriteria.from( Customer.class );
			updateCriteria.set( Customer_.name, "Bob" );
			updateCriteria.set( Customer_.age, 99 );
			entityManager.createQuery( updateCriteria ).executeUpdate();

		} );
}

	@Test
	public void testJoinsAndFetchesDisallowed(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {

		CriteriaBuilder builder = entityManager.getCriteriaBuilder();

		{
			try {
				CriteriaDelete<Customer> deleteCriteria = builder.createCriteriaDelete( Customer.class );
				Root<Customer> root = deleteCriteria.from( Customer.class );
				root.join( Customer_.spouse );
				entityManager.createQuery( deleteCriteria ).executeUpdate();
				fail( "Expected failure due to attempt to join" );
			}
			catch (IllegalArgumentException expected) {
			}
		}

			assertThrows(
					IllegalArgumentException.class,
					() -> {
						CriteriaDelete<Customer> deleteCriteria = builder.createCriteriaDelete( Customer.class );
						Root<Customer> root = deleteCriteria.from( Customer.class );
						root.fetch( Customer_.spouse );
						entityManager.createQuery( deleteCriteria ).executeUpdate();
					},
					"Expected failure due to attempt to fetch"
			);

			assertThrows(
					IllegalArgumentException.class,
					() -> {
						CriteriaUpdate<Customer> updateCriteria = builder.createCriteriaUpdate( Customer.class );
						Root<Customer> root = updateCriteria.from( Customer.class );
						root.join( Customer_.spouse );
						entityManager.createQuery( updateCriteria ).executeUpdate();
					},
					"Expected failure due to attempt to join"
			);

			assertThrows(
					IllegalArgumentException.class,
					() -> {
						CriteriaUpdate<Customer> updateCriteria = builder.createCriteriaUpdate( Customer.class );
						Root<Customer> root = updateCriteria.from( Customer.class );
						root.fetch( Customer_.spouse );
						entityManager.createQuery( updateCriteria ).executeUpdate();
					},
					"Expected failure due to attempt to fetch"
			);

		} );
	}

	@Test
	// MySQL does not allow "delete/update from" and subqueries to use the same table
	@SkipForDialect(dialectClass = MySQLDialect.class, matchSubTypes = true)
	public void testDeleteWithUnCorrelatedSubquery(EntityManagerFactoryScope scope) {
		CriteriaBuilder builder = scope.getEntityManagerFactory().getCriteriaBuilder();
		scope.inTransaction( entityManager -> {

			// attempt to delete Customers whose age is less than the AVG age
			CriteriaDelete<Customer> criteria = builder.createCriteriaDelete( Customer.class );
			Root<Customer> customerRoot = criteria.from( Customer.class );

			Subquery<Double> subCriteria = criteria.subquery( Double.class );
			Root<Customer> subQueryCustomerRoot = subCriteria.from( Customer.class );
			subCriteria.select( builder.avg( subQueryCustomerRoot.get( Customer_.age ) ) );

			// also illustrates the new capability to use the subquery selection as an expression!
			criteria.where(
					builder.lessThan(
							customerRoot.get( Customer_.age ),
							subCriteria.as( Integer.class )
					)
			);

			// make sure Subquery#getParent fails...
			assertThrows(
					IllegalStateException.class,
					subCriteria::getParent,
					"Expecting Subquery.getParent call to fail on DELETE containing criteria"
			);

			Query query = entityManager.createQuery( criteria );
			assertThrows(
					IllegalStateException.class,
					query::getResultList,
					"Attempt to getResultList() on delete criteria should have failed"
			);
			query.executeUpdate();
		} );
	}

}
