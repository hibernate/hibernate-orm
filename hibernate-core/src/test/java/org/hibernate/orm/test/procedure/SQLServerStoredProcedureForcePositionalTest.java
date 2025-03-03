/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.procedure;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.SQLServerDialect;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.Table;

import static org.hibernate.testing.transaction.TransactionUtil.doInAutoCommit;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(value = SQLServerDialect.class, majorVersion = 11)
@Jpa(
		annotatedClasses = {
				Person.class,
				Phone.class,
				SQLServerStoredProcedureForcePositionalTest.Address.class
		},
		properties = @Setting( name = AvailableSettings.QUERY_PASS_PROCEDURE_PARAMETER_NAMES, value = "false")
)
public class SQLServerStoredProcedureForcePositionalTest {

	private static final String CITY = "London";
	private static final String STREET = "Lollard Street";
	private static final String ZIP = "SE116UG";

	@BeforeEach
	public void init(EntityManagerFactoryScope scope) {
		doInAutoCommit(
				"DROP PROCEDURE sp_count_phones",
				"CREATE PROCEDURE sp_count_phones " +
						"   @personId INT, " +
						"   @phoneCount INT OUTPUT " +
						"AS " +
						"BEGIN " +
						"   SELECT @phoneCount = COUNT(*)  " +
						"   FROM Phone  " +
						"   WHERE person_id = @personId " +
						"END"
		);

		scope.inTransaction( entityManager -> {
			Person person1 = new Person( 1L, "John Doe" );
			person1.setNickName( "JD" );
			person1.setAddress( "Earth" );
			person1.setCreatedOn( Timestamp.from( LocalDateTime.of( 2000, 1, 1, 0, 0, 0 )
														.toInstant( ZoneOffset.UTC ) ) );

			entityManager.persist( person1 );

			Phone phone1 = new Phone( "123-456-7890" );
			phone1.setId( 1L );

			person1.addPhone( phone1 );

			Phone phone2 = new Phone( "098_765-4321" );
			phone2.setId( 2L );

			person1.addPhone( phone2 );

			Address address = new Address( 1l, STREET, CITY, ZIP );
			entityManager.persist( address );
		} );
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.releaseEntityManagerFactory();
	}

	@Test
	public void testStoredProcedure(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_count_phones" );
			query.registerStoredProcedureParameter( "personId2", Long.class, ParameterMode.IN );
			query.registerStoredProcedureParameter( "phoneCount", Long.class, ParameterMode.OUT );

			query.setParameter( "personId2", 1L );

			query.execute();
			Long phoneCount = (Long) query.getOutputParameterValue( "phoneCount" );
			assertEquals( Long.valueOf( 2 ), phoneCount );
		} );
	}

	@Entity(name = "Address")
	@Table(name = "ADDRESS_TABLE")
	public static class Address {
		@Id
		@Column(name = "ID")
		private long id;
		@Column(name = "STREET")
		private String street;
		@Column(name = "CITY")
		private String city;
		@Column(name = "ZIP")
		private String zip;

		public Address() {
		}

		public Address(long id, String street, String city, String zip) {
			this.id = id;
			this.street = street;
			this.city = city;
			this.zip = zip;
		}

		public long getId() {
			return id;
		}

		public String getStreet() {
			return street;
		}

		public String getCity() {
			return city;
		}

		public String getZip() {
			return zip;
		}
	}
}
