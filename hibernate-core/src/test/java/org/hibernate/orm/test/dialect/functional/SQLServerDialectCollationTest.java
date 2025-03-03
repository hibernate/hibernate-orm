/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.functional;

import java.io.Serializable;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.SQLServerDialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Test;


import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 * used driver hibernate.connection.driver_class com.microsoft.sqlserver.jdbc.SQLServerDriver
 *
 * @author Guenther Demetz
 */
@RequiresDialect(SQLServerDialect.class)
public class SQLServerDialectCollationTest extends BaseCoreFunctionalTestCase {

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.setProperty( AvailableSettings.KEYWORD_AUTO_QUOTING_ENABLED, Boolean.TRUE.toString() );
	}

	@Override
	protected void buildSessionFactory() {
		BootstrapServiceRegistry bootRegistry = buildBootstrapServiceRegistry();
		StandardServiceRegistryImpl _serviceRegistry =
				buildServiceRegistry( bootRegistry, constructAndConfigureConfiguration( bootRegistry ) );

		try {
			try {
				TransactionUtil.doWithJDBC(
						_serviceRegistry,
						connection -> {
							try (Statement statement = connection.createStatement()) {
								connection.setAutoCommit( true );
								statement.executeUpdate( "DROP DATABASE hibernate_orm_test_collation" );
							}
						}
				);
			}
			catch (SQLException e) {
				log.debug( e.getMessage() );
			}
			try {
				TransactionUtil.doWithJDBC(
						_serviceRegistry,
						connection -> {
							try (Statement statement = connection.createStatement()) {
								connection.setAutoCommit( true );
								statement.executeUpdate( "CREATE DATABASE hibernate_orm_test_collation COLLATE Latin1_General_CS_AS" );
								statement.executeUpdate( "ALTER DATABASE [hibernate_orm_test_collation] SET AUTO_CLOSE OFF " );
							}
						}
				);
			}
			catch (SQLException e) {
				log.debug( e.getMessage() );
			}
		}
		finally {
			_serviceRegistry.destroy();
		}
		super.buildSessionFactory();
	}

	@Test
	@JiraKey(value = "HHH-7198")
	public void testMaxResultsSqlServerWithCaseSensitiveCollation() throws Exception {
		doInHibernate( this::sessionFactory, session -> {
			for ( int i = 1; i <= 20; i++ ) {
				session.persist( new CustomProduct( i, "Kit" + i ) );
			}
			session.flush();
			session.clear();

			List list = session.createQuery( "from CustomProduct where description like 'Kit%'" )
					.setFirstResult( 2 )
					.setMaxResults( 2 )
					.list();
			assertEquals( 2, list.size() );
		} );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
			CustomProduct.class
		};
	}


	@Entity(name = "CustomProduct")
	@Table(catalog = "hibernate_orm_test_collation", schema = "dbo")
	public static class CustomProduct implements Serializable {
		@Id
		public Integer id;

		@Column(name = "description", nullable = false)
		public String description;

		public CustomProduct() {
		}

		public CustomProduct(Integer id, String description) {
			this.id = id;
			this.description = description;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			CustomProduct that = (CustomProduct) o;
			return Objects.equals( description, that.description );
		}

		@Override
		public int hashCode() {
			return Objects.hash( description );
		}

		@Override
		public String toString() {
			return "CustomProduct{" +
					"id=" + id +
					", description='" + description + '\'' +
					'}';
		}
	}

	@Override
	protected boolean rebuildSessionFactoryOnError() {
		return false;
	}
}
