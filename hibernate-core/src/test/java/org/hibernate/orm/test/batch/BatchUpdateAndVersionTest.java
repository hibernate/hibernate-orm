/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;


import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.RollbackException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.exception.SnapshotIsolationException;
import org.hibernate.exception.TransactionSerializationException;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hibernate.testing.orm.junit.DialectContext.getDialect;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@DomainModel(
		annotatedClasses = {
				BatchUpdateAndVersionTest.EntityA.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "2")
)
public class BatchUpdateAndVersionTest {

	@Test
	@JiraKey(value = "HHH-16394")
	public void testUpdate(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
		scope.inTransaction(
				session -> {
					EntityA entityA1 = new EntityA( 1 );
					EntityA entityA2 = new EntityA( 2 );
					EntityA ownerA2 = new EntityA( 3 );

					session.persist( ownerA2 );
					session.persist( entityA1 );
					session.persist( entityA2 );

					session.flush();

					entityA1.setPropertyA( 3 );
					entityA2.addOwner( ownerA2 );
				}
		);

		scope.inTransaction(
				session -> {
					EntityA entityA1 = session.get( EntityA.class, 1 );
					assertThat( entityA1.getVersion() ).isEqualTo( 1 );
					assertThat( entityA1.getPropertyA() ).isEqualTo( 3 );
					assertThat( entityA1.getOwners().size() ).isEqualTo( 0 );

					EntityA entityA2 = session.get( EntityA.class, 2 );
					assertThat( entityA2.getVersion() ).isEqualTo( 1 );
					assertThat( entityA2.getPropertyA() ).isEqualTo( 0 );

					List<EntityA> owners = entityA2.getOwners();
					assertThat( owners.size() ).isEqualTo( 1 );

					EntityA ownerA2 = owners.get( 0 );
					assertThat( ownerA2.getId() ).isEqualTo( 3 );
					assertThat( ownerA2.getPropertyA() ).isEqualTo( 0 );
					assertThat( ownerA2.getOwners().size() ).isEqualTo( 0 );
				}
		);
	}

	@Test
	public void testFailedUpdate(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
		scope.inTransaction(
				session -> {
					EntityA entityA1 = new EntityA( 1 );
					EntityA entityA2 = new EntityA( 2 );
					EntityA ownerA2 = new EntityA( 3 );
					session.persist( ownerA2 );
					session.persist( entityA1 );
					session.persist( entityA2 );
				}
		);

		try {
			scope.inTransaction(
					session1 -> {
						EntityA entityA1_1 = session1.get( EntityA.class, 1 );
						assertThat( entityA1_1.getVersion() ).isEqualTo( 0 );
						assertThat( entityA1_1.getPropertyA() ).isEqualTo( 0 );

						EntityA entityA2_1 = session1.get( EntityA.class, 2 );
						assertThat( entityA2_1.getVersion() ).isEqualTo( 0 );
						assertThat( entityA2_1.getPropertyA() ).isEqualTo( 0 );

						scope.inTransaction(
								session2 -> {
									EntityA entityA1_2 = session2.get( EntityA.class, 1 );
									assertThat( entityA1_2.getVersion() ).isEqualTo( 0 );
									assertThat( entityA1_2.getPropertyA() ).isEqualTo( 0 );

									EntityA entityA2_2 = session2.get( EntityA.class, 2 );
									assertThat( entityA2_2.getVersion() ).isEqualTo( 0 );
									assertThat( entityA2_2.getPropertyA() ).isEqualTo( 0 );

									entityA1_2.setPropertyA( 5 );
									entityA2_2.setPropertyA( 5 );
								}
						);

						entityA1_1.setPropertyA( 3 );
						entityA2_1.setPropertyA( 3 );
					}
			);
			fail();
		}
		catch (OptimisticLockException ole) {
			if (getDialect() instanceof MariaDBDialect && getDialect().getVersion().isAfter( 11, 6, 2 )) {
				// if @@innodb_snapshot_isolation is set, database throw an exception if record is not available anymore
				assertTrue( ole.getCause() instanceof SnapshotIsolationException );
			} else {
				assertTrue( ole.getCause() instanceof StaleObjectStateException );
			}
		}
		//CockroachDB errors with a Serialization Exception
		catch (RollbackException rbe) {
			assertTrue( rbe.getCause() instanceof TransactionSerializationException );
		}
	}

	@Entity(name = "EntityA")
	@Table(name = "ENTITY_A")
	public static class EntityA {

		@Id
		@Column(name = "ID")
		Integer id;

		@Version
		@Column(name = "VERSION")
		long version;

		@Column(name = "PROPERTY_A")
		int propertyA;

		@ManyToMany
		@JoinTable(name = "ENTITY_A_TO_ENTITY_A", inverseJoinColumns = @JoinColumn(name = "SIDE_B"), joinColumns = @JoinColumn(name = "SIDE_A"))
		final List<EntityA> owners = new ArrayList<>();

		public EntityA() {
		}

		public EntityA(Integer id) {
			this.id = id;
		}

		public void setPropertyA(int propertyA) {
			this.propertyA = propertyA;
		}

		public void addOwner(EntityA owner) {
			owners.add( owner );
		}

		public Integer getId() {
			return id;
		}

		public long getVersion() {
			return version;
		}

		public int getPropertyA() {
			return propertyA;
		}

		public List<EntityA> getOwners() {
			return owners;
		}
	}

}
