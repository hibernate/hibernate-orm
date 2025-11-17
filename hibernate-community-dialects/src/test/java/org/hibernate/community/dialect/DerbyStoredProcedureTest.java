/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Consumer;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.Query;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;

@Jpa(
		annotatedClasses = DerbyStoredProcedureTest.Person.class,
		properties = @Setting(name = AvailableSettings.JPA_LOAD_BY_ID_COMPLIANCE, value = "true")
)
@RequiresDialect(value = DerbyDialect.class)
public class DerbyStoredProcedureTest {

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		createProcedures( scope.getEntityManagerFactory(), this::createProcedureSelectBydate );
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		createProcedures( scope.getEntityManagerFactory(), this::dropProcedures );
	}

	@Test
	public void testCreateNotExistingStoredProcedureQuery(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Assertions.assertThrows(
							IllegalArgumentException.class,
							() -> {
								entityManager.createStoredProcedureQuery(
												"NOT_EXISTING_NAME",
												"NOT_EXISTING_RESULT_MAPPING"
										)
										.execute();
							}
					);
				} );
	}

	@Test
	public void testSetDateParameter(EntityManagerFactoryScope scope) throws Exception {
		SimpleDateFormat formatter = new SimpleDateFormat( "yyyy-MM-dd" );
		final Date d2 = formatter.parse( "2001-06-27" );
		final Date d3 = formatter.parse( "2002-07-07" );
		final Date d4 = formatter.parse( "2003-03-03" );
		final Date d5 = new Date();
		scope.inTransaction(
				entityManager -> {

					entityManager.persist( new Person( 1, "Andrea", new Date() ) );
					entityManager.persist( new Person( 2, "Luigi", d2 ) );
					entityManager.persist( new Person( 3, "Massimiliano", d3 ) );
					entityManager.persist( new Person( 4, "Elisabetta", d4 ) );
					entityManager.persist( new Person( 5, "Roberto", d5 ) );
				}
		);

		scope.inTransaction(
				entityManager -> {
					StoredProcedureQuery storedProcedureQuery = entityManager.createStoredProcedureQuery( "SelectByDate" );
					storedProcedureQuery.registerStoredProcedureParameter( 1, Date.class, ParameterMode.IN );
					storedProcedureQuery.registerStoredProcedureParameter( 2, Integer.class, ParameterMode.OUT );
					storedProcedureQuery.setParameter( 1, new Date(), TemporalType.DATE );
					assertFalse( storedProcedureQuery.execute() );

					Object outputParameterValue = storedProcedureQuery.getOutputParameterValue( 2 );
					assertInstanceOf( Integer.class, outputParameterValue );
					assertEquals( 1, outputParameterValue );

					storedProcedureQuery = entityManager.createStoredProcedureQuery( "SelectByDate" );
					storedProcedureQuery.registerStoredProcedureParameter( 1, Date.class, ParameterMode.IN );
					storedProcedureQuery.registerStoredProcedureParameter( 2, Integer.class, ParameterMode.OUT );
					storedProcedureQuery.setParameter( 1, new Date(), TemporalType.DATE );

					assertFalse( storedProcedureQuery.execute() );
					outputParameterValue = storedProcedureQuery.getOutputParameterValue( 2 );
					assertInstanceOf( Integer.class, outputParameterValue );
					assertEquals( 1, outputParameterValue );
				}
		);
	}

	@Test
	public void testSetWrongParameterPosition(EntityManagerFactoryScope scope) {

		scope.inTransaction(
				entityManager -> {
					StoredProcedureQuery storedProcedureQuery = entityManager
							.createStoredProcedureQuery( "SelectByDate" );
					storedProcedureQuery.registerStoredProcedureParameter( 1, Date.class, ParameterMode.IN );
					try {
						storedProcedureQuery.setParameter( 2, new Date(), TemporalType.DATE );
						fail( "IllegalArgumentException expected" );
					}
					catch (IllegalArgumentException e) {
						// ecxpected
					}

					storedProcedureQuery = entityManager.createStoredProcedureQuery( "SelectByDate" );
					storedProcedureQuery.registerStoredProcedureParameter( 1, Date.class, ParameterMode.IN );
					Query q1 = storedProcedureQuery.setParameter( 1, new Date() );
					try {
						q1.setParameter( 2, new Date(), TemporalType.DATE );
						fail( "IllegalArgumentException expected" );
					}
					catch (IllegalArgumentException e) {
						//expected
					}
				}
		);
	}


	private void createProcedures(EntityManagerFactory emf, Consumer<Statement> consumer) {
		try {
			TransactionUtil.doWithJDBC(
					emf.unwrap( SessionFactoryImplementor.class ).getServiceRegistry(),
					connection -> {
						connection.setAutoCommit( false );

						try (Statement statement = connection.createStatement()) {
							consumer.accept( statement );
						}
						connection.commit();
					}
			);
		}
		catch (SQLException e) {
			throw new RuntimeException( "Unable to create stored procedures", e );
		}
	}

	private void dropProcedures(Statement statement) {
		try {
			statement.execute( "DROP PROCEDURE SelectByDate" );
		}
		catch (SQLException e) {
			throw new RuntimeException( e );
		}
	}


	private void createProcedureSelectBydate(Statement statement) {
		try {
			statement.execute(
					"CREATE PROCEDURE SelectByDate(in IN_PARAM DATE, out OUT_PARAMAM INTEGER) " +
							"language java external name 'org.hibernate.community.dialect.DerbyStoredProcedureTest.selectByDate' " +
							"parameter style java"
			);
		}
		catch (SQLException e) {
			throw new RuntimeException( e );
		}
	}

	public static void selectByDate(java.sql.Date inParam, int[] outParam) throws SQLException {
		try (Connection con = DriverManager.getConnection( "jdbc:default:connection" )) {
			Statement stmt = con.createStatement();
			try (ResultSet rs = stmt.executeQuery( "SELECT ID FROM PERSON_TABLE where DATE_OF_BIRTH='" + inParam + "'" )) {
				if ( rs.next() ) {
					outParam[0] = rs.getInt( 1 );
				}
			}
		}
	}


	@Entity(name = "Person")
	@Table(name = "PERSON_TABLE")
	public static class Person {

		@Id
		private Integer id;

		private String name;

		@Column(name = "DATE_OF_BIRTH")
		@Temporal(TemporalType.DATE)
		private Date dateOfBirth;

		public Person() {
		}

		public Person(Integer id, String name, Date dateOfBirth) {
			this.id = id;
			this.name = name;
			this.dateOfBirth = dateOfBirth;
		}
	}

}
