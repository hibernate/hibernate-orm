/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.dialect.resolver;

import java.util.Map;
import java.util.Properties;
import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Id;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.EntityManagerFactoryBasedFunctionalTest;
import org.hibernate.testing.util.ReflectionUtil;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-13253")
@RequiresDialect( H2Dialect.class )
public class JpaDialectResolverTest extends EntityManagerFactoryBasedFunctionalTest {

	private static Properties environmentProperties;

	private static String dialectSetting;

	@BeforeAll
	public static void init() {
		environmentProperties = ReflectionUtil.getStaticFieldValue( Environment.class, "GLOBAL_PROPERTIES" );
		if ( environmentProperties.containsKey( AvailableSettings.DIALECT ) ) {
			dialectSetting = (String) environmentProperties.remove( AvailableSettings.DIALECT );
		}
	}

	@AfterAll
	public static void destroy() {
		if ( dialectSetting != null ) {
			environmentProperties.put( AvailableSettings.DIALECT, dialectSetting );
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Book.class
		};
	}

	@Override
	protected Map buildSettings() {
		Map settings = super.buildSettings();
		settings.remove( AvailableSettings.DIALECT );
		settings.put( AvailableSettings.HBM2DDL_DB_NAME, "H2" );

		Properties properties = ReflectionUtil.getStaticFieldValue( Environment.class, "GLOBAL_PROPERTIES" );
		properties.remove( AvailableSettings.DIALECT );

		return settings;
	}

	@Test
	public void test() throws Exception {
		EntityManagerFactory entityManagerFactory = entityManagerFactory();
		SessionFactoryImplementor sessionFactoryImplementor = entityManagerFactory.unwrap( SessionFactoryImplementor.class );
		Dialect dialect = sessionFactoryImplementor.getServiceRegistry().getService( JdbcServices.class ).getDialect();
		assertTrue( H2Dialect.class.isAssignableFrom( dialect.getClass() ) );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Book book1 = new Book();
			book1.setId( 1L );
			book1.setTitle( "High-Performance Java Persistence" );
			book1.setAuthor( "Vlad Mihalcea" );

			Book book2 = new Book();
			book2.setId( 2L );
			book2.setTitle( "Java Persistence with Hibernate" );
			book2.setAuthor( "Gavin King" );

			entityManager.persist( book1 );
			entityManager.persist( book2 );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Book book1 = entityManager.find( Book.class, 1L );
			assertEquals( "High-Performance Java Persistence", book1.getTitle() );
			assertEquals( "Vlad Mihalcea", book1.getAuthor() );
		} );
	}

	@Entity(name = "Book")
	public static class Book {

		@Id
		private Long id;

		private String title;

		private String author;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getAuthor() {
			return author;
		}

		public void setAuthor(String author) {
			this.author = author;
		}
	}
}
