/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.functional;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.internal.util.PropertiesHelper;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-12973")
@RequiresDialect(value = MariaDBDialect.class)
@Jpa(
		annotatedClasses = {
				SequenceInformationMariaDBTest.Book.class,
				SequenceInformationMariaDBTest.Author.class
		},
		integrationSettings = {
				@Setting(name = AvailableSettings.HBM2DDL_AUTO, value = "none")
		}
)
public class SequenceInformationMariaDBTest {

	private DriverManagerConnectionProviderImpl connectionProvider;

	@BeforeAll
	public void init() {
		connectionProvider = new DriverManagerConnectionProviderImpl();
		connectionProvider.configure( PropertiesHelper.map( Environment.getProperties() ) );

		try(Connection connection = connectionProvider.getConnection();
			Statement statement = connection.createStatement()) {
			try {
				statement.execute( "DROP SEQUENCE IF EXISTS book_sequence" );
				statement.execute( "DROP SEQUENCE IF EXISTS author_sequence" );
			}
			catch (SQLException e) {

			}
			try {
				statement.execute( "DROP TABLE TBL_BOOK" );
				statement.execute( "DROP TABLE TBL_AUTHOR" );
			}
			catch (SQLException e) {
			}
			statement.execute( "CREATE TABLE `TBL_BOOK` ( " +
							"  `ID` int(11) NOT NULL, " +
							"  `TITLE` varchar(255) DEFAULT NULL, " +
							"   PRIMARY KEY (`ID`) " +
							") ENGINE=InnoDB" );

			statement.execute( "CREATE TABLE `TBL_AUTHOR` ( " +
									"  `ID` int(11) NOT NULL, " +
									"  `firstName` varchar(255) DEFAULT NULL, " +
									"  `lastName` varchar(255) DEFAULT NULL, " +
									"   PRIMARY KEY (`ID`) " +
									") ENGINE=InnoDB" );

			statement.execute( "CREATE SEQUENCE book_sequence " +
							"  START WITH 1 " +
							"  INCREMENT BY 1 " +
							"  MAXVALUE 2999999999 " +
							"  MINVALUE 0 " +
							"  CACHE 10" );

			statement.execute( "CREATE SEQUENCE author_sequence " +
							"  START WITH 1 " +
							"  INCREMENT BY 1 " +
							"  MAXVALUE 2999999999 " +
							"  MINVALUE 0 " +
							"  CACHE 10" );
		}
		catch (SQLException e) {
			fail(e.getMessage());
		}
	}

	@AfterAll
	public void releaseResources() {
		try(Connection connection = connectionProvider.getConnection();
			Statement statement = connection.createStatement()) {
			try {
				statement.execute( "DROP SEQUENCE book_sequence" );
				statement.execute( "DROP SEQUENCE author_sequence" );
			}
			catch (SQLException e) {
			}
			try {
				statement.execute( "DROP TABLE TBL_BOOK" );
				statement.execute( "DROP TABLE TBL_AUTHOR" );
			}
			catch (SQLException e) {
			}
		}
		catch (SQLException e) {
			fail(e.getMessage());
		}

		if ( connectionProvider != null ) {
			connectionProvider.stop();
		}
	}

	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Book book = new Book();
					book.setTitle("My Book");

					entityManager.persist(book);
				}
		);
	}

	@Entity
	@Table(name = "TBL_BOOK")
	public static class Book {

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		@SequenceGenerator(
				name = "book_sequence",
				allocationSize = 10
		)
		@Column(name = "ID")

		private Integer id;

		@Column(name = "TITLE")
		private String title;

		public Book() {
		}

		public Book(String title) {
			this.title = title;

		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		@Override
		public String toString() {
			return "Book{" +
					"id=" + id +
					", title='" + title + '\'' +
					'}';
		}
	}

	@Entity
	@Table(name = "TBL_AUTHOR")
	public static class Author {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		@SequenceGenerator(
				name = "author_sequence",
				allocationSize = 10
		)
		@Column(name = "ID")
		private Integer id;

		@Column(name = "FIRST_NAME")
		private String firstName;

		@Column(name = "LAST_NAME")
		private String lastName;
	}
}
