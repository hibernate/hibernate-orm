/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.functional;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.SQLServerDialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * used driver hibernate.connection.driver_class com.microsoft.sqlserver.jdbc.SQLServerDriver
 *
 * @author Guenther Demetz
 */
@RequiresDialect(SQLServerDialect.class)
@DomainModel(annotatedClasses = {SQLServerDialectCollationTest.CustomProduct.class})
@SessionFactory(exportSchema = false)
@ServiceRegistry(settings = {@Setting(name = AvailableSettings.KEYWORD_AUTO_QUOTING_ENABLED, value = "true")})
public class SQLServerDialectCollationTest {

	private static final String TABLE_NAME = "CustomProduct";
	private static final String CATALOG = "hibernate_orm_test_collation";


	@BeforeEach
	protected void setup(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.doWork( connection -> {
				try (PreparedStatement ps = connection.prepareStatement( "DROP DATABASE IF EXISTS " + CATALOG )) {
					ps.execute();
				}
			} );
		} );

		scope.inTransaction( session -> {
			session.doWork( connection -> {
				try (Statement statement = connection.createStatement()) {
					statement.executeUpdate( "CREATE DATABASE " + CATALOG + " COLLATE Latin1_General_CS_AS" );
					statement.executeUpdate( "ALTER DATABASE [" + CATALOG + "] SET AUTO_CLOSE OFF " );
				}
			} );
		} );

		scope.getSessionFactory().getSchemaManager().exportMappedObjects( false );
	}

	@Test
	@JiraKey(value = "HHH-7198")
	public void testMaxResultsSqlServerWithCaseSensitiveCollation(SessionFactoryScope scope) {
		scope.inTransaction(  session -> {
			for ( int i = 1; i <= 20; i++ ) {
				session.persist( new CustomProduct( i, "Kit" + i ) );
			}
		} );

		scope.inTransaction(   session -> {
			List<CustomProduct> list = session.createQuery( "from CustomProduct where description like 'Kit%'", CustomProduct.class )
					.setFirstResult( 2 )
					.setMaxResults( 2 )
					.list();
			assertEquals( 2, list.size() );
		} );
	}

	@Entity(name = TABLE_NAME)
	@Table(catalog = CATALOG, schema = "dbo")
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

}
