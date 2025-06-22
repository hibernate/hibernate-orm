/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetoone.formula;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;

import org.hibernate.Hibernate;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.jdbc.ClobJdbcType;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gavin King
 */
public class OneToOneFormulaTest extends BaseSessionFactoryFunctionalTest {
	private static class TextAsMaterializedClobType extends AbstractSingleColumnStandardBasicType<String> {
		public final static TextAsMaterializedClobType INSTANCE = new TextAsMaterializedClobType();

		public TextAsMaterializedClobType() {
			super( ClobJdbcType.DEFAULT, StringJavaType.INSTANCE );
		}

		public String getName() {
			return "text";
		}
	}

	@Override
	protected void applySettings(StandardServiceRegistryBuilder builder) {
		builder.applySetting( Environment.USE_SECOND_LEVEL_CACHE, "false" );
		builder.applySetting( Environment.GENERATE_STATISTICS, "true" );
		builder.applySetting( Environment.DEFAULT_BATCH_FETCH_SIZE, "2" );
	}

	@Override
	protected void applyMetadataBuilder(MetadataBuilder metadataBuilder) {
		if ( OracleDialect.class.isInstance( getDialect() ) ) {
			metadataBuilder.applyBasicType( TextAsMaterializedClobType.INSTANCE );
		}
	}

	@Override
	public String[] getOrmXmlFiles() {
		return new String[] { "org/hibernate/orm/test/onetoone/formula/Person.hbm.xml" };
	}


	private Person person;
	private Address address;

	@BeforeEach
	public void setUp() {
		person = new Person();
		person.setName( "Gavin King" );
		address = new Address();
		address.setPerson( person );
		address.setType( "HOME" );
		address.setZip( "3181" );
		address.setState( "VIC" );
		address.setStreet( "Karbarook Ave" );
		person.setAddress( address );

		inTransaction(
				session ->
						session.persist( person )
		);
	}

	@AfterEach
	protected void cleanupTest() {
		inTransaction( session -> {
			session.remove( person );
		} );
	}

	@Test
	public void testOneToOneFormula() {

		inTransaction( session -> {
			Person p = (Person) session.createQuery( "from Person" ).uniqueResult();

			assertNotNull( p.getAddress() );
			assertTrue( Hibernate.isInitialized( p.getAddress() ) );
			assertNull( p.getMailingAddress() );
		} );

		inTransaction( session -> {
			Person p = (Person) session.createQuery(
					"from Person p left join fetch p.mailingAddress left join fetch p.address" ).uniqueResult();

			assertNotNull( p.getAddress() );
			assertTrue( Hibernate.isInitialized( p.getAddress() ) );
			assertNull( p.getMailingAddress() );
		} );

		inTransaction( session -> {
			Person p = (Person) session.createQuery( "from Person p left join fetch p.address" ).uniqueResult();

			assertNotNull( p.getAddress() );
			assertTrue( Hibernate.isInitialized( p.getAddress() ) );
			assertNull( p.getMailingAddress() );
		} );

		inTransaction( session -> {
			CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			CriteriaQuery<Person> criteria = criteriaBuilder.createQuery( Person.class );
			Root<Person> root = criteria.from( Person.class );
			Join<Object, Object> address = root.join( "address", JoinType.INNER );
			criteria.where( criteriaBuilder.equal( address.get( "zip" ), "3181" ) );
			Person p = session.createQuery( criteria ).uniqueResult();

			assertNotNull( p );
		} );

		inTransaction( session -> {
			CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			CriteriaQuery<Person> criteria = criteriaBuilder.createQuery( Person.class );
			Root<Person> root = criteria.from( Person.class );
			root.fetch( "address", JoinType.LEFT );
			Person p = session.createQuery( criteria ).uniqueResult();

			assertNotNull( p.getAddress() );
			assertTrue( Hibernate.isInitialized( p.getAddress() ) );
			assertNull( p.getMailingAddress() );
		} );

		inTransaction( session -> {
			CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			CriteriaQuery<Person> criteria = criteriaBuilder.createQuery( Person.class );
			Root<Person> root = criteria.from( Person.class );
			root.fetch( "address", JoinType.LEFT );
			Person p = session.createQuery( criteria ).uniqueResult();

			assertNotNull( p.getAddress() );
			assertTrue( Hibernate.isInitialized( p.getAddress() ) );
			assertNull( p.getMailingAddress() );

		} );

		inTransaction( session -> {
			Address a = (Address) session.createQuery( "from Address" ).uniqueResult();

			Person person = a.getPerson();
			assertNotNull( person );
			assertTrue( Hibernate.isInitialized( person.getAddress() ) );
			assertTrue( Hibernate.isInitialized( person.getMailingAddress() ) );
			assertNull( person.getMailingAddress() );
		} );
	}


	@Test
	@JiraKey(value = "HHH-5757")
	public void testQuery() {
		inTransaction( session -> {
			Person p = (Person) session.createQuery( "from Person p where p.address = :address" ).setParameter(
					"address",
					address
			).uniqueResult();
			assertThat( p, notNullValue() );
		} );

		inTransaction( session -> {
			Address a = (Address) session.createQuery( "from Address a where a.person = :person" ).setParameter(
					"person",
					person
			).uniqueResult();
			assertThat( a, notNullValue() );

		} );
	}

	@Test
	public void testOneToOneEmbeddedCompositeKey() {
		inTransaction( session -> {
			Address a = new Address();
			a.setType( "HOME" );
			a.setPerson( person );
			a = session.getReference( Address.class, a );
			assertFalse( Hibernate.isInitialized( a ) );
			a.getPerson();
			a.getType();
			assertFalse( Hibernate.isInitialized( a ) );
			assertEquals( "3181", a.getZip() );
		} );

		inTransaction( session -> {
			Address a = new Address();
			a.setType( "HOME" );
			a.setPerson( person );
			Address a2 = session.get( Address.class, a );
			assertTrue( Hibernate.isInitialized( a ) );
			assertSame( a, a2 );
			assertSame( person, a2.getPerson() ); //this is a little bit desirable
			assertEquals( "3181", a.getZip() );
		} );


//		s.remove(a2);
//		s.remove( s.get( Person.class, p.getName() ) ); //this is certainly undesirable! oh well...
//
//		t.commit();
//		s.close();

	}

}
