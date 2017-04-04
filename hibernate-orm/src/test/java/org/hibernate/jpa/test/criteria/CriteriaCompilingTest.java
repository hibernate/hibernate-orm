/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.EntityType;

import org.hibernate.CacheMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.jpa.test.callbacks.RemoteControl;
import org.hibernate.jpa.test.callbacks.Television;
import org.hibernate.jpa.test.callbacks.VideoSystem;
import org.hibernate.jpa.test.inheritance.Fruit;
import org.hibernate.jpa.test.inheritance.Strawberry;
import org.hibernate.jpa.test.metamodel.Address;
import org.hibernate.jpa.test.metamodel.Alias;
import org.hibernate.jpa.test.metamodel.Country;
import org.hibernate.jpa.test.metamodel.CreditCard;
import org.hibernate.jpa.test.metamodel.Customer;
import org.hibernate.jpa.test.metamodel.Info;
import org.hibernate.jpa.test.metamodel.LineItem;
import org.hibernate.jpa.test.metamodel.Order;
import org.hibernate.jpa.test.metamodel.Phone;
import org.hibernate.jpa.test.metamodel.Product;
import org.hibernate.jpa.test.metamodel.ShelfLife;
import org.hibernate.jpa.test.metamodel.Spouse;

import org.hibernate.testing.BeforeClassOnce;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Steve Ebersole
 */
public class CriteriaCompilingTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Customer.class,
				Alias.class,
				Phone.class,
				Address.class,
				Country.class,
				CreditCard.class,
				Info.class,
				Spouse.class,
				LineItem.class,
				Order.class,
				Product.class,
				ShelfLife.class,
				// @Inheritance
				Fruit.class,
				Strawberry.class,
				// @MappedSuperclass
				VideoSystem.class,
				Television.class,
				RemoteControl.class
		};
	}

	@Before
	public void setUp(){
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			Customer customer = new Customer();
			customer.setId( "id" );
			customer.setName( " David R. Vincent " );
			entityManager.persist( customer );
			customer = new Customer();
			customer.setId( "id2" );
			customer.setName( "R Vincent" );
			entityManager.persist( customer );
		} );
	}

	@Test
	public void testTrim() {
		final String expectedResult = "David R. Vincent";

		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {

			CriteriaBuilder cb = entityManager.getCriteriaBuilder();

			CriteriaQuery<String> cquery = cb.createQuery( String.class );
			Root<Customer> cust = cquery.from( Customer.class );

			//Get Metamodel from Root
			EntityType<Customer> Customer_ = cust.getModel();

			cquery.where(
					cb.equal(
							cust.get( Customer_.getSingularAttribute( "name", String.class ) ),
							cb.literal( " David R. Vincent " )
					)
			);
			cquery.select(
					cb.trim(
							CriteriaBuilder.Trimspec.BOTH,
							cust.get( Customer_.getSingularAttribute( "name", String.class ) )
					)
			);

			TypedQuery<String> tq = entityManager.createQuery( cquery );

			String result = tq.getSingleResult();
			Assert.assertEquals( "Mismatch in received results", expectedResult, result );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11393")
	public void testTrimAChar() {
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
			final CriteriaQuery<Customer> query = criteriaBuilder.createQuery( Customer.class );
			final Root<Customer> from = query.from( Customer.class );
			query.select( from );

			query.where( criteriaBuilder.equal( criteriaBuilder.trim(
					CriteriaBuilder.Trimspec.LEADING,
					criteriaBuilder.literal( 'R' ),
					from.get( "name" )
			), " Vincent" ) );
			List<Customer> resultList = entityManager.createQuery( query ).getResultList();
			assertThat( resultList.size(), is( 1 ) );
		} );
	}

	@Test
	public void testJustSimpleRootCriteria() {
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			// First w/o explicit selection...
			CriteriaQuery<Customer> criteria = entityManager.getCriteriaBuilder().createQuery( Customer.class );
			criteria.from( Customer.class );
			entityManager.createQuery( criteria ).getResultList();

			// Now with...
			criteria = entityManager.getCriteriaBuilder().createQuery( Customer.class );
			Root<Customer> root = criteria.from( Customer.class );
			criteria.select( root );
			entityManager.createQuery( criteria ).getResultList();
		});
	}

	@Test
	public void testSimpleJoinCriteria() {
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {

			// String based...
			CriteriaQuery<Order> criteria = entityManager.getCriteriaBuilder().createQuery( Order.class );
			Root<Order> root = criteria.from( Order.class );
			root.join( "lineItems" );
			criteria.select( root );
			entityManager.createQuery( criteria ).getResultList();
		});
	}

	@Test
	public void testSimpleFetchCriteria() {
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {

			// String based...
			CriteriaQuery<Order> criteria = entityManager.getCriteriaBuilder().createQuery( Order.class );
			Root<Order> root = criteria.from( Order.class );
			root.fetch( "lineItems" );
			criteria.select( root );
			entityManager.createQuery( criteria ).getResultList();
		} );
	}

	@Test
	public void testSerialization() {
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {

			CriteriaQuery<Order> criteria = entityManager.getCriteriaBuilder().createQuery( Order.class );
			Root<Order> root = criteria.from( Order.class );
			root.fetch( "lineItems" );
			criteria.select( root );

			criteria = serializeDeserialize( criteria );

			entityManager.createQuery( criteria ).getResultList();
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10960")
	public void testDeprecation() {
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {

			Session session = entityManager.unwrap( Session.class );
			CriteriaBuilder builder = session.getCriteriaBuilder();
			CriteriaQuery<Order> query = builder.createQuery( Order.class );
			Root<Order> from = query.from( Order.class );
			query.orderBy( builder.desc( from.get( "totalPrice" ) ) );
			TypedQuery<Order> jpaQuery = session.createQuery( query );
			org.hibernate.query.Query<?> hibQuery = jpaQuery.unwrap( org.hibernate.query.Query.class );

			ScrollableResults sr = hibQuery.scroll( ScrollMode.FORWARD_ONLY );

			hibQuery.setCacheMode( CacheMode.IGNORE ).scroll( ScrollMode.FORWARD_ONLY );

			org.hibernate.query.Query<Order> anotherQuery = session.createQuery(
					"select o from Order o where totalPrice in :totalPrices",
					Order.class
			);
			anotherQuery.setParameterList( "totalPrices", Arrays.asList( 12.5d, 14.6d ) );
		});
	}

	@SuppressWarnings( {"unchecked"})
	private <T> T serializeDeserialize(T object) {
		T serializedObject = null;
		try {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			ObjectOutput out = new ObjectOutputStream( stream );
			out.writeObject( object );
			out.close();
			byte[] serialized = stream.toByteArray();
			stream.close();
			ByteArrayInputStream byteIn = new ByteArrayInputStream( serialized );
			ObjectInputStream in = new ObjectInputStream( byteIn );
			serializedObject = (T) in.readObject();
			in.close();
			byteIn.close();
		}
		catch (Exception e) {
			Assert.fail( "Unable to serialize / deserialize the object: " + e.getMessage() );
		}
		return serializedObject;
	}

	@Override
	public void releaseResources() {
		super.releaseResources();
	}
}
