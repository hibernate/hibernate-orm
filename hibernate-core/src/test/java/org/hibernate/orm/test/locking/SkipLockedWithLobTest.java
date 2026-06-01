/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

import org.hibernate.Timeouts;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Reproducer for ORA-00600 [kollAdjustDuration - compile time VBL, run time RBL]
 * triggered by SELECT ... FOR UPDATE ... SKIP LOCKED on a table with a LOB column
 * when the dialect generates {@code lob(col) query as value} DDL (Oracle 23+).
 *
 * @see OracleDialect#supportsValueLOBAccess()
 */
@RequiresDialect(OracleDialect.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSkipLocked.class)
@DomainModel(annotatedClasses = SkipLockedWithLobTest.OutboxEvent.class)
@SessionFactory
@ServiceRegistry( // TODO: remove once the issue is fixed on the Oracle side
		settings = @Setting(
				name = "hibernate.dialect.oracle.value_lob_enabled",
				value = "false"
		)
)
public class SkipLockedWithLobTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( int i = 1; i <= 5; i++ ) {
				OutboxEvent event = new OutboxEvent();
				event.setId( (long) i );
				event.setEntityName( "Entity_" + i );
				event.setPayload( ("payload-" + i).getBytes() );
				session.persist( event );
			}
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void testSelectForUpdateSkipLockedWithLobColumn(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			List<OutboxEvent> events = session.createQuery(
							"select e from OutboxEvent e where e.id in (:ids)",
							OutboxEvent.class
					)
					.setParameter( "ids", List.of( 1L, 2L, 3L ) )
					.setHibernateLockMode( org.hibernate.LockMode.PESSIMISTIC_WRITE )
					.setLockTimeout( Timeouts.SKIP_LOCKED )
					.getResultList();

			assertEquals( 3, events.size() );
			for ( OutboxEvent event : events ) {
				assertNotNull( event.getPayload() );
			}
		} );
	}

	@Entity(name = "OutboxEvent")
	public static class OutboxEvent {

		@Id
		private Long id;

		private String entityName;

		@Lob
		private byte[] payload;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getEntityName() {
			return entityName;
		}

		public void setEntityName(String entityName) {
			this.entityName = entityName;
		}

		public byte[] getPayload() {
			return payload;
		}

		public void setPayload(byte[] payload) {
			this.payload = payload;
		}
	}
}
