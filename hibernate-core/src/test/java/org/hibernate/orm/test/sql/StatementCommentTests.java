/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql;

import org.hibernate.LockMode;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry(
		settings = @Setting( name = AvailableSettings.USE_SQL_COMMENTS, value = "true" )
)
@DomainModel( annotatedClasses = StatementCommentTests.VersionedEntity.class )
@SessionFactory( useCollectingStatementInspector = true )
public class StatementCommentTests {
	@Test
	public void testEntityMutationComments(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		// insert
		scope.inTransaction( (session) -> {
			session.persist( new VersionedEntity( 1, "tbd" ) );
		} );
		checkEntityComments( inspector );

		// update
		scope.inTransaction( (session) -> {
			final VersionedEntity entity = session.find( VersionedEntity.class, 1 );
			inspector.clear();
			entity.setName( "The One" );
		} );
		checkEntityComments( inspector );

		// forced version increment
		scope.inTransaction( (session) -> {
			final VersionedEntity entity = session.find( VersionedEntity.class, 1 );
			inspector.clear();
			session.lock( entity, LockMode.OPTIMISTIC_FORCE_INCREMENT );
		} );
		checkEntityComments( inspector );

		// delete
		scope.inTransaction( (session) -> {
			final VersionedEntity entity = session.find( VersionedEntity.class, 1 );
			inspector.clear();
			session.remove( entity );
		} );
		checkEntityComments( inspector );
	}

	private void checkEntityComments(SQLStatementInspector inspector) {
		assertThat( inspector.getSqlQueries() ).hasSize( 1 );
		assertThat( inspector.getSqlQueries().get( 0 ) ).contains( "VersionedEntity */" );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity( name = "VersionedEntity" )
	@Table( name = "entity_table" )
	public static class VersionedEntity {
		@Id
		private Integer id;
		@Version
		private Integer version;
		@Basic
		private String name;

		protected VersionedEntity() {
			// for use by Hibernate
		}

		public VersionedEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Integer getVersion() {
			return version;
		}
	}
}
