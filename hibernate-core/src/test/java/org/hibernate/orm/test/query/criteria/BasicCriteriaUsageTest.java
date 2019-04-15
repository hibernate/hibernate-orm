/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.criteria;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.boot.MetadataSources;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.retail.Name;
import org.hibernate.testing.orm.domain.retail.Order;
import org.hibernate.testing.orm.domain.retail.SalesAssociate;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public class BasicCriteriaUsageTest extends SessionFactoryBasedFunctionalTest {

	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		StandardDomainModel.RETAIL.getDescriptor().applyDomainModel( metadataSources );
	}

	@AfterEach
	public void cleanUpData() {
		inTransaction(
				session -> {
					// some tests create SalesAssociate data
					session.createQuery( "delete from SalesAssociate" ).executeUpdate();
				}
		);
	}

	@Test
	public void testParameterCollection() {
		inTransaction(
				session -> {
					final SqmSelectStatement<Order> criteria = session.getCriteriaBuilder().createQuery( Order.class );
					final SqmRoot<Order> root = criteria.from( Order.class );
					final SqmParameter<Integer> parameter = session.getCriteriaBuilder().parameter( Integer.class );
					criteria.where( session.getCriteriaBuilder().equal( root.get( "id" ), parameter ) );
					assertThat( criteria.getParameters().size(), is( 1 ) );
				}
		);
	}

	@Test
	public void testTrivialCompilation() {
		inTransaction(
				session -> {
					final SqmSelectStatement<Order> criteria = session.getCriteriaBuilder().createQuery( Order.class );
					final SqmRoot<Order> root = criteria.from( Order.class );
					criteria.select( root );
					session.createQuery( criteria ).list();
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8283")
	@FailureExpected( "custom composite user type" )
	public void testDateCompositeCustomType() {
		throw new NotYetImplementedFor6Exception();
//		Payment payment = new Payment();
//		payment.setAmount( new BigDecimal( 1000 ) );
//		payment.setDate( new Date() );
//
//		EntityManager em = getOrCreateEntityManager();
//		em.getTransaction().begin();
//		em.persist( payment );
//
//		CriteriaQuery<Payment> criteria = em.getCriteriaBuilder().createQuery( Payment.class );
//		Root<Payment> rp = criteria.from( Payment.class );
//		Predicate predicate = em.getCriteriaBuilder().equal( rp.get( Payment_.date ), new Date() );
//		criteria.where( predicate );
//
//		TypedQuery<Payment> q = em.createQuery( criteria );
//		List<Payment> payments = q.getResultList();
//
//		assertEquals( 1, payments.size() );
//
//		em.getTransaction().commit();
//		em.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8373")
	public void testFunctionCriteria() {
		inTransaction(
				session -> {
					session.save( new SalesAssociate( 1, new Name( "Quest", "Jonny" ) ) );
				}
		);

		inTransaction(
				session -> {
					NodeBuilder cb = session.getCriteriaBuilder();
					final SqmSelectStatement<SalesAssociate> criteria = cb.createQuery( SalesAssociate.class );
					final SqmRoot<SalesAssociate> root = criteria.from( SalesAssociate.class );
					criteria.select( root );

					criteria.where(
							cb.equal(
									cb.lower( root.get( "name" ).get( "familyName" ) ),
									cb.literal( "quest" )
							)
					);

					final SalesAssociate result = session.createQuery( criteria ).getSingleResult();
					assertThat( result, notNullValue() );
				}
		);
	}


	@Test
	@FailureExpected( "Support for SqmTuple not implemented in conversion to SQL" )
	public void testTupleExpression() {
		// todo (6.0) : really want this to work as well.  it might already actually - checking :)
		inTransaction(
				session -> {
					NodeBuilder cb = session.getCriteriaBuilder();
					final SqmSelectStatement<SalesAssociate> criteria = cb.createQuery( SalesAssociate.class );
					final SqmRoot<SalesAssociate> root = criteria.from( SalesAssociate.class );
					criteria.select( root );

					criteria.where(
							cb.equal(
									root.get( "name" ),
									cb.tuple( Object.class, cb.literal( "Quest" ), cb.literal( "Jonny" ) )
							)
					);

					final SalesAssociate result = session.createQuery( criteria ).getSingleResult();
					assertThat( result, notNullValue() );
				}
		);
	}
    
	@Test
	@TestForIssue( jiraKey = "HHH-8914" )
	public void testDoubleNegation() {
		inTransaction(
				session -> {
					session.save( new SalesAssociate( 1, new Name( "Quest", "Jonny" ) ) );
				}
		);

		// Although the examples are contrived and the usages appear pointless,
		// double negatives can occur in some dynamic applications (regardless
		// if it results from bad design or not).  Ensure we handle them as expected.

		inTransaction(
				session -> {
					final NodeBuilder cb = session.getCriteriaBuilder();

					final SqmSelectStatement<Integer> criteria = cb.createQuery( Integer.class );
					final SqmRoot<SalesAssociate> root = criteria.from( SalesAssociate.class );
					criteria.select( root.get( "id" ) );

					criteria.where(
							cb.not( cb.isNull( root.get( "id" ) ) )
					);
					assertThat(
							session.createQuery( criteria ).getSingleResult(),
							notNullValue()
					);

					criteria.where(
							cb.not( cb.not( cb.isNotNull( root.get( "id" ) ) ) )
					);
					assertThat(
							session.createQuery( criteria ).getSingleResult(),
							notNullValue()
					);

					criteria.where(
							cb.not(
									cb.not(
											cb.not(
													cb.isNull( root.get( "id" ) )
											)
									)

							)
					);
					assertThat(
							session.createQuery( criteria ).getSingleResult(),
							notNullValue()
					);
				}
		);
	}
}
