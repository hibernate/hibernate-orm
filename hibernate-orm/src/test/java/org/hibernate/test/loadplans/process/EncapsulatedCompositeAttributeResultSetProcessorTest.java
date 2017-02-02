/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.loadplans.process;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.jdbc.Work;
import org.hibernate.loader.JoinWalker;
import org.hibernate.loader.entity.EntityJoinWalker;
import org.hibernate.loader.plan.exec.process.spi.ResultSetProcessor;
import org.hibernate.loader.plan.exec.query.spi.NamedParameterContext;
import org.hibernate.loader.plan.exec.spi.LoadQueryDetails;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.OuterJoinLoadable;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.junit4.ExtraAssertions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * @author Gail Badner
 */
public class EncapsulatedCompositeAttributeResultSetProcessorTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Person.class, Customer.class };
	}

	@Test
	public void testSimpleNestedCompositeAttributeProcessing() throws Exception {
		// create some test data
		Session session = openSession();
		session.beginTransaction();
		Person person = new Person();
		person.id = 1;
		person.name = "Joe Blow";
		person.address = new Address();
		person.address.address1 = "1313 Mockingbird Lane";
		person.address.city = "Pleasantville";
		person.address.country = "USA";
		AddressType addressType = new AddressType();
		addressType.typeName = "snail mail";
		person.address.type = addressType;
		session.save( person );
		session.getTransaction().commit();
		session.close();

//		session = openSession();
//		session.beginTransaction();
//		Person personGotten = (Person) session.get( Person.class, person.id );
//		assertEquals( person.id, personGotten.id );
//		assertEquals( person.address.address1, personGotten.address.address1 );
//		assertEquals( person.address.city, personGotten.address.city );
//		assertEquals( person.address.country, personGotten.address.country );
//		assertEquals( person.address.type.typeName, personGotten.address.type.typeName );
//		session.getTransaction().commit();
//		session.close();

		List results = getResults( sessionFactory().getEntityPersister( Person.class.getName() ) );
		assertEquals( 1, results.size() );
		Object result = results.get( 0 );
		assertNotNull( result );

		Person personWork = ExtraAssertions.assertTyping( Person.class, result );
		assertEquals( person.id, personWork.id );
		assertEquals( person.address.address1, personWork.address.address1 );
		assertEquals( person.address.city, personWork.address.city );
		assertEquals( person.address.country, personWork.address.country );
		assertEquals( person.address.type.typeName, person.address.type.typeName );

		// clean up test data
		session = openSession();
		session.beginTransaction();
		session.createQuery( "delete Person" ).executeUpdate();
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testNestedCompositeElementCollectionQueryBuilding() {
		doCompare(
				sessionFactory(),
				(OuterJoinLoadable) sessionFactory().getClassMetadata( Customer.class )
		);
	}

	private void doCompare(SessionFactoryImplementor sf, OuterJoinLoadable persister) {
		final LoadQueryInfluencers influencers = LoadQueryInfluencers.NONE;
		final LockMode lockMode = LockMode.NONE;
		final int batchSize = 1;

		final EntityJoinWalker walker = new EntityJoinWalker(
				persister,
				persister.getKeyColumnNames(),
				batchSize,
				lockMode,
				sf,
				influencers
		);

		final LoadQueryDetails details = Helper.INSTANCE.buildLoadQueryDetails( persister, sf );

		compare( walker, details );
	}

	private void compare(JoinWalker walker, LoadQueryDetails details) {
		System.out.println( "WALKER    : " + walker.getSQLString() );
		System.out.println( "LOAD-PLAN : " + details.getSqlStatement() );
		System.out.println();
	}

	@Test
	public void testNestedCompositeElementCollectionProcessing() throws Exception {
		// create some test data
		Session session = openSession();
		session.beginTransaction();
		Person person = new Person();
		person.id = 1;
		person.name = "Joe Blow";
		session.save( person );
		Customer customer = new Customer();
		customer.id = 1L;
		Investment investment1 = new Investment();
		investment1.description = "stock";
		investment1.date = new Date();
		investment1.monetaryAmount = new MonetaryAmount();
		investment1.monetaryAmount.currency = MonetaryAmount.CurrencyCode.USD;
		investment1.monetaryAmount.amount = BigDecimal.valueOf( 1234, 2 );
		investment1.performedBy = person;
		Investment investment2 = new Investment();
		investment2.description = "bond";
		investment2.date = new Date();
		investment2.monetaryAmount = new MonetaryAmount();
		investment2.monetaryAmount.currency = MonetaryAmount.CurrencyCode.EUR;
		investment2.monetaryAmount.amount = BigDecimal.valueOf( 98176, 1 );
		customer.investments.add( investment1 );
		customer.investments.add( investment2 );
		session.save( customer );
		session.getTransaction().commit();
		session.close();

//		session = openSession();
//		session.beginTransaction();
//		Customer customerGotten = (Customer) session.get( Customer.class, customer.id );
//		assertEquals( customer.id, customerGotten.id );
//		session.getTransaction().commit();
//		session.close();

		List results = getResults( sessionFactory().getEntityPersister( Customer.class.getName() ) );

		assertEquals( 2, results.size() );
		assertSame( results.get( 0 ), results.get( 1 ) );
		Object result = results.get( 0 );
		assertNotNull( result );

		Customer customerWork = ExtraAssertions.assertTyping( Customer.class, result );

		// clean up test data
		session = openSession();
		session.beginTransaction();
		session.delete( customerWork.investments.get( 0 ).performedBy );
		session.delete( customerWork );
		session.getTransaction().commit();
		session.close();
	}

	private List<?> getResults(EntityPersister entityPersister ) {
		final LoadPlan plan = Helper.INSTANCE.buildLoadPlan( sessionFactory(), entityPersister );

		final LoadQueryDetails queryDetails = Helper.INSTANCE.buildLoadQueryDetails( plan, sessionFactory() );
		final String sql = queryDetails.getSqlStatement();
		final ResultSetProcessor resultSetProcessor = queryDetails.getResultSetProcessor();

		final List results = new ArrayList();

		final Session workSession = openSession();
		workSession.beginTransaction();
		workSession.doWork(
				new Work() {
					@Override
					public void execute(Connection connection) throws SQLException {
						PreparedStatement ps = connection.prepareStatement( sql );
						ps.setInt( 1, 1 );
						ResultSet resultSet = ps.executeQuery();
						results.addAll(
								resultSetProcessor.extractResults(
										resultSet,
										(SessionImplementor) workSession,
										new QueryParameters(),
										new NamedParameterContext() {
											@Override
											public int[] getNamedParameterLocations(String name) {
												return new int[0];
											}
										},
										true,
										false,
										null,
										null
								)
						);
						resultSet.close();
						ps.close();
					}
				}
		);
		workSession.getTransaction().commit();
		workSession.close();
		return results;
	}

	@Entity( name = "Person" )
	public static class Person implements Serializable {
		@Id
		Integer id;
		String name;

		@Embedded
		Address address;
	}

	@Embeddable
	public static class Address implements Serializable {
		String address1;
		String city;
		String country;
		AddressType type;
	}

	@Embeddable
	public static class AddressType {
		String typeName;
	}

	@Entity( name = "Customer" )
	public static class Customer {
		private Long id;
		private List<Investment> investments = new ArrayList<Investment>();

		@Id
		public Long getId() {
			return id;
		}
		public void setId(Long id) {
			this.id = id;
		}

		@ElementCollection(fetch = FetchType.EAGER)
		@CollectionTable( name = "investments", joinColumns = @JoinColumn( name = "customer_id" ) )
		public List<Investment> getInvestments() {
			return investments;
		}
		public void setInvestments(List<Investment> investments) {
			this.investments = investments;
		}
	}

	@Embeddable
	public static class Investment {
		private MonetaryAmount monetaryAmount;
		private String description;
		private Date date;
		private Person performedBy;

		@Embedded
		public MonetaryAmount getMonetaryAmount() {
			return monetaryAmount;
		}
		public void setMonetaryAmount(MonetaryAmount monetaryAmount) {
			this.monetaryAmount = monetaryAmount;
		}
		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}
		@Column(name="`date`")
		public Date getDate() {
			return date;
		}
		public void setDate(Date date) {
			this.date = date;
		}
		@ManyToOne
		public Person getPerformedBy() {
			return performedBy;
		}
		public void setPerformedBy(Person performedBy) {
			this.performedBy = performedBy;
		}
	}

	@Embeddable
	public static class MonetaryAmount {
		public static enum CurrencyCode {
			USD,
			EUR
		}
		private BigDecimal amount;
		@Column(length = 3)
		@Enumerated(EnumType.STRING)
		private CurrencyCode currency;

		public BigDecimal getAmount() {
			return amount;
		}
		public void setAmount(BigDecimal amount) {
			this.amount = amount;
		}

		public CurrencyCode getCurrency() {
			return currency;
		}
		public void setCurrency(CurrencyCode currency) {
			this.currency = currency;
		}
	}
}
