/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.dialect.functional;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.MariaDB103Dialect;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-12973")
@RequiresDialect(MariaDB103Dialect.class)
public class SequenceInformationMariaDBTest extends
		BaseEntityManagerFunctionalTestCase {

	private DriverManagerConnectionProviderImpl connectionProvider;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Book.class,
				Author.class
		};
	}

	@Override
	public void buildEntityManagerFactory() {
		connectionProvider = new DriverManagerConnectionProviderImpl();
		connectionProvider.configure( Environment.getProperties() );

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

		super.buildEntityManagerFactory();
	}

	@Override
	public void releaseResources() {
		super.releaseResources();

		super.releaseResources();

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

	@Override
	protected void addMappings(Map settings) {
		settings.put( AvailableSettings.HBM2DDL_AUTO, "none" );
	}

	@Test
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Book book = new Book();
			book.setTitle("My Book");

			entityManager.persist(book);
		} );
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
