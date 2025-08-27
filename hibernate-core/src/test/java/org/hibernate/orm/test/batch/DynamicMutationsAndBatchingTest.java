/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.Transaction;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Jan Schatteman
 */
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "2"),
		}
)
@DomainModel(
		annotatedClasses = { DynamicMutationsAndBatchingTest.EntityA.class }
)
@SessionFactory
@JiraKey( value = "HHH-16352")
public class DynamicMutationsAndBatchingTest {

	@AfterEach
	public void cleanup( SessionFactoryScope scope ) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testDynamicInserts( SessionFactoryScope scope ) {
		scope.inTransaction(
				s -> {
					EntityA entityA1 = new EntityA(1);
					entityA1.propertyA = 1;

					EntityA entityA2 = new EntityA(2);
					entityA2.propertyB = 2;
					entityA2.propertyC = 3;

					EntityA entityA3 = new EntityA(3);
					entityA3.propertyA = 4;
					entityA3.propertyC = 5;

					s.persist(entityA1);
					s.persist(entityA2);
					s.persist(entityA3);
				}
		);

		scope.inTransaction(
				s -> {
					Query<EntityA> query = s.createQuery( "select e from EntityA e where id = :id", EntityA.class);
					EntityA a1 = query.setParameter( "id", 1 ).uniqueResult();
					assertNotNull( a1 );
					assertNull(a1.propertyB);
					assertNull(a1.propertyC);
					assertEquals( 1, a1.propertyA );

					EntityA a2 = query.setParameter( "id", 2 ).uniqueResult();
					assertNotNull( a2 );
					assertNull( a2.propertyA );
					assertEquals( 2, a2.propertyB );
					assertEquals( 3, a2.propertyC );

					EntityA a3 = query.setParameter( "id", 3 ).uniqueResult();
					assertNotNull( a3 );
					assertNull( a3.propertyB );
					assertEquals( 4, a3.propertyA );
					assertEquals( 5, a3.propertyC );
				}
		);
	}

	@Test
	public void testDynamicUpdates( SessionFactoryScope scope ) {
		scope.inTransaction(
				s -> {
					EntityA entityA1 = new EntityA(1);
					EntityA entityA2 = new EntityA(2);
					EntityA entityA3 = new EntityA(3);
					s.persist(entityA1);
					s.persist(entityA2);
					s.persist(entityA3);
				}
		);

		scope.inSession(
				s -> {
					Query<EntityA> query = s.createQuery( "select e from EntityA e order by id asc", EntityA.class);
					Transaction tx = s.beginTransaction();
					List<EntityA> actual = query.list();
					actual.get(0).propertyA = 1;
					actual.get(1).propertyA = 2;
					actual.get(1).propertyB = 2;
					actual.get(2).propertyA = 4;
					s.flush();
					tx.commit();
				}
		);

		scope.inTransaction(
				s -> {
					Query<EntityA> query = s.createQuery( "select e from EntityA e where id = :id", EntityA.class);
					EntityA a1 = query.setParameter( "id", 1 ).uniqueResult();
					assertNotNull( a1 );
					assertEquals( 1, a1.propertyA );
					assertNull(a1.propertyB);
					assertNull(a1.propertyC);

					EntityA a2 = query.setParameter( "id", 2 ).uniqueResult();
					assertNotNull( a2 );
					assertNull( a2.propertyC );
					assertEquals( 2, a2.propertyA );
					assertEquals( 2, a2.propertyB );

					EntityA a3 = query.setParameter( "id", 3 ).uniqueResult();
					assertNotNull( a3 );
					assertNull( a3.propertyB );
					assertNull( a3.propertyC );
					assertEquals( 4, a3.propertyA );
				}
		);
	}

	@DynamicInsert
	@DynamicUpdate
	@Entity(name="EntityA")
	@Table(name = "ENTITY_A")
	public static class EntityA {

		@Id
		@Column(name = "ID")
		Integer id;

		@Column(name = "PROPERTY_A")
		Integer propertyA;

		@Column(name = "PROPERTY_B")
		Integer propertyB;

		@Column(name = "PROPERTY_C")
		Integer propertyC;

		public EntityA() {
		}

		public EntityA(Integer id) {
			this.id = id;
		}
	}
}
