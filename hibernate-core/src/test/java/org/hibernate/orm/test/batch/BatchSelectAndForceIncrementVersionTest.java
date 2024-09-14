/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.batch;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.Version;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author Steve Dighans
 */
@SessionFactory
@DomainModel(annotatedClasses = {
		BatchSelectAndForceIncrementVersionTest.VersionEntity.class
})
@ServiceRegistry(settings = @Setting(name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "2"))
@JiraKey("HHH-16939")
public class BatchSelectAndForceIncrementVersionTest {
	@Test
	public void testBatchPessimisticForceIncrement(SessionFactoryScope scope) {
		final String searchProperty = "A";

		scope.inTransaction( session -> {
			VersionEntity entityA = new VersionEntity();
			entityA.setSearchProperty( searchProperty );

			session.persist( entityA );
		} );

		scope.inTransaction( session -> {
			TypedQuery<VersionEntity> q = findBySearchPropertyTypedQuery( session, searchProperty );
			q.setLockMode( LockModeType.PESSIMISTIC_FORCE_INCREMENT );

			// SELECT the row, version should already be incremented and will UPDATE when the transaction is committed.
			VersionEntity result = q.getSingleResult();

			assertThat( result.getVersion() ).as(
					"Batch pessimistic force increment version is not 1, before the transaction committed." ).isEqualTo(
					1L );
		} );

		scope.inTransaction( session -> {
			TypedQuery<VersionEntity> q = findBySearchPropertyTypedQuery( session, searchProperty );

			// SELECT without a lock mode to check the value, which should be 1 now.
			VersionEntity result = q.getSingleResult();

			assertThat( result.getVersion() ).as(
							"Batch pessimistic force increment version is not 1, after the transaction committed." )
					.isEqualTo( 1L );
		} );
	}

	@Test
	public void testBatchOptimisticForceIncrement(SessionFactoryScope scope) {
		final String searchProperty = "B";

		scope.inTransaction( session -> {
			VersionEntity entityA = new VersionEntity();
			entityA.setSearchProperty( searchProperty );

			session.persist( entityA );
		} );

		scope.inTransaction( session -> {
			TypedQuery<VersionEntity> q = findBySearchPropertyTypedQuery( session, searchProperty );
			q.setLockMode( LockModeType.OPTIMISTIC_FORCE_INCREMENT );

			// SELECT the row, it should be still version 0.
			// It will increment and UPDATE when the transaction is committed.
			VersionEntity result = q.getSingleResult();

			assertThat( result.getVersion() ).as(
					"Batch optimistic force increment version is not 0, before the transaction committed." ).isEqualTo(
					0L );
		} );

		scope.inTransaction( session -> {
			TypedQuery<VersionEntity> q = findBySearchPropertyTypedQuery( session, searchProperty );

			// SELECT without a lock mode to check the value, which should be 1 now.
			VersionEntity result = q.getSingleResult();

			assertThat( result.getVersion() ).as(
							"Batch optimistic force increment version is not 1, after the transaction committed." )
					.isEqualTo( 1L );
		} );
	}

	protected TypedQuery<VersionEntity> findBySearchPropertyTypedQuery(Session session, String searchProperty) {
		return session.createQuery(
						"SELECT v FROM VersionEntity v WHERE v.searchProperty = :searchProperty",
						VersionEntity.class
				)
				.setParameter( "searchProperty", searchProperty );
	}

	@Entity(name = "VersionEntity")
	@Table(name = "VERSION_ENTITY")
	public static class VersionEntity {

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "versionEntitySeq")
		@SequenceGenerator(name = "versionEntitySeq", sequenceName = "VERSION_ENTITY_SEQ", allocationSize = 1)
		@Column(name = "ID")
		private Long id;

		@Version
		@Column(name = "VERSION")
		private Integer version;

		@Column(name = "SEARCH_PROPERTY")
		private String searchProperty;

		public VersionEntity() {
		}

		public Long getId() {
			return id;
		}

		public Integer getVersion() {
			return version;
		}

		public String getSearchProperty() {
			return searchProperty;
		}

		public void setSearchProperty(String searchProperty) {
			this.searchProperty = searchProperty;
		}
	}
}
