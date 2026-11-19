/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.dirty;

import java.util.Objects;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = VersionedPersistTest.VersionedEntity.class)
@SessionFactory(useCollectingStatementInspector = true)
@BytecodeEnhanced(runNotEnhancedAsWell = true, testEnhancedClasses = VersionedPersistTest.VersionedEntity.class)
@JiraKey("HHH-20757")
class VersionedPersistTest {

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void persistUnchangedEntity(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();
		final var entity = new VersionedEntity();
		entity.setCode( "initial" );
		scope.inTransaction( session -> {
			session.persist( entity );
			final Long initialVersion = entity.getVersion();
			assertThat( initialVersion ).isNotNull();
			inspector.clear();

			session.flush();
			inspector.assertExecutedCount( 1 );
			inspector.assertIsInsert( 0 );
			assertThat( entity.getVersion() ).isEqualTo( initialVersion );

			inspector.clear();
			session.flush();
			inspector.assertExecutedCount( 0 );
			assertThat( entity.getVersion() ).isEqualTo( initialVersion );
		} );
		inspector.assertExecutedCount( 0 );
		scope.inTransaction( session -> {
			final var reloaded = session.find( VersionedEntity.class, entity.getId() );
			assertThat( reloaded.getCode() ).isEqualTo( "initial" );
			assertThat( reloaded.getVersion() ).isEqualTo( entity.getVersion() );
		} );
	}

	@Test
	void persistThenModifyEntity(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();
		final var entity = new VersionedEntity();
		entity.setCode( "initial" );
		scope.inTransaction( session -> {
			session.persist( entity );
			final Long initialVersion = entity.getVersion();
			entity.setCode( "changed" );
			inspector.clear();

			session.flush();
			inspector.assertExecutedCount( 2 );
			inspector.assertIsInsert( 0 );
			inspector.assertIsUpdate( 1 );
			assertThat( entity.getVersion() ).isEqualTo( initialVersion + 1 );
		} );
		scope.inTransaction( session -> {
			final var reloaded = session.find( VersionedEntity.class, entity.getId() );
			assertThat( reloaded.getCode() ).isEqualTo( "changed" );
			assertThat( reloaded.getVersion() ).isEqualTo( entity.getVersion() );
		} );
	}

	@Test
	void modifyAfterCleanFlush(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();
		final var entity = new VersionedEntity();
		entity.setCode( "initial" );
		scope.inTransaction( session -> {
			session.persist( entity );
			final Long initialVersion = entity.getVersion();
			inspector.clear();

			session.flush();
			inspector.assertExecutedCount( 1 );
			inspector.assertIsInsert( 0 );
			assertThat( entity.getVersion() ).isEqualTo( initialVersion );

			entity.setCode( "changed-after-flush" );
			inspector.clear();
			session.flush();
			inspector.assertExecutedCount( 1 );
			inspector.assertIsUpdate( 0 );
			assertThat( entity.getVersion() ).isEqualTo( initialVersion + 1 );
		} );
		scope.inTransaction( session -> {
			final var reloaded = session.find( VersionedEntity.class, entity.getId() );
			assertThat( reloaded.getCode() ).isEqualTo( "changed-after-flush" );
			assertThat( reloaded.getVersion() ).isEqualTo( entity.getVersion() );
		} );
	}

	@Test
	void persistWithVersionSetterSideEffect(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();
		final var entity = new VersionedEntity();
		entity.setCode( "initial" );
		entity.changeCodeOnVersionSeed = true;
		scope.inTransaction( session -> {
			session.persist( entity );
			final Long initialVersion = entity.getVersion();
			assertThat( entity.getCode() ).isEqualTo( "version-seeded" );
			inspector.clear();

			session.flush();
			inspector.assertExecutedCount( 2 );
			inspector.assertIsInsert( 0 );
			inspector.assertIsUpdate( 1 );
			assertThat( entity.getVersion() ).isEqualTo( initialVersion + 1 );

			inspector.clear();
			session.flush();
			inspector.assertExecutedCount( 0 );
		} );
		scope.inTransaction( session -> {
			final var reloaded = session.find( VersionedEntity.class, entity.getId() );
			assertThat( reloaded.getCode() ).isEqualTo( "version-seeded" );
			assertThat( reloaded.getVersion() ).isEqualTo( entity.getVersion() );
		} );
	}

	@Entity(name = "VersionedEntity")
	@Access(AccessType.PROPERTY)
	@DynamicUpdate
	public static class VersionedEntity {
		private Long id;
		private Long version;
		private String code;
		@Transient
		private boolean changeCodeOnVersionSeed;

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			if ( !Objects.equals( this.id, id ) ) {
				this.id = id;
			}
		}

		@Version
		public Long getVersion() {
			return version;
		}

		public void setVersion(Long version) {
			if ( !Objects.equals( this.version, version ) ) {
				final boolean versionSeeded = this.version == null && version != null;
				this.version = version;
				if ( changeCodeOnVersionSeed && versionSeeded ) {
					setCode( "version-seeded" );
				}
			}
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			if ( !Objects.equals( this.code, code ) ) {
				this.code = code;
			}
		}
	}
}
