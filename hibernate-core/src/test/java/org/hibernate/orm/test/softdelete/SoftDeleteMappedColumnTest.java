/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.softdelete;

import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.schema.Action;

import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Marco Belladelli
 */
@Jira( "https://hibernate.atlassian.net/browse/HHH-17461" )
public class SoftDeleteMappedColumnTest {
	@Test
	public void testValid() {
		try (final SessionFactory sf = buildSessionFactory( ValidEntity.class )) {
			sf.inTransaction( session -> {
				final ValidEntity validEntity = new ValidEntity( 1L, "valid1" );
				session.persist( validEntity );
				session.flush();
				assertThat( validEntity.isDeleted() ).isFalse();
				session.remove( validEntity );
			} );
			sf.inSession( session -> assertThat( session.find( ValidEntity.class, 1L ) ).isNull() );
		}
	}

	@Test
	public void testInvalid() {
		try (final SessionFactory sf = buildSessionFactory( InvalidEntity.class )) {
			sf.inTransaction( session -> {
				final InvalidEntity entity = new InvalidEntity( 2L, "invalid2" );
				session.persist( entity );
			} );
			fail( "Duplicate soft-delete column should fail" );
		}
		catch (Exception e) {
			assertThat( e ).isInstanceOf( MappingException.class );
			assertThat( e.getMessage() ).contains( "Column 'is_deleted' is duplicated" );
		}
	}

	private SessionFactory buildSessionFactory(Class<?> entityClass) {
		final Configuration cfg = new Configuration()
				.setProperty( AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, Action.ACTION_CREATE_THEN_DROP )
				.addAnnotatedClass( entityClass );
		ServiceRegistryUtil.applySettings( cfg.getStandardServiceRegistryBuilder() );
		return cfg.buildSessionFactory();
	}

	@Entity( name = "ValidEntity" )
	@SoftDelete( columnName = "is_deleted" )
	public static class ValidEntity {
		@Id
		private Long id;

		private String name;

		@Column( name = "is_deleted", insertable = false, updatable = false )
		private boolean deleted;

		public ValidEntity() {
		}

		public ValidEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public boolean isDeleted() {
			return deleted;
		}
	}

	@Entity( name = "InvalidEntity" )
	@SoftDelete( columnName = "is_deleted" )
	public static class InvalidEntity {
		@Id
		private Long id;

		private String name;

		@Column( name = "is_deleted" )
		private boolean deleted;

		public InvalidEntity() {
		}

		public InvalidEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
