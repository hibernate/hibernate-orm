/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batchfetch;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.annotations.Fetch;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Jpa(
		annotatedClasses = {
				BatchingEntityLoaderInitializationWithNoLockModeTest.MainEntity.class,
				BatchingEntityLoaderInitializationWithNoLockModeTest.SubEntity.class
		},
		properties = { @Setting(name = AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, value = "5") }
)
public class BatchingEntityLoaderInitializationWithNoLockModeTest {

	private Long mainId;


	@Test
	public void testJoin(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			SubEntity sub = new SubEntity();
			em.persist( sub );

			MainEntity main = new MainEntity();
			main.setSub( sub );
			em.persist( main );

			this.mainId = main.getId();
		} );

		scope.inTransaction( em -> {
			final EntityPersister entityPersister = em.getEntityManagerFactory()
					.unwrap( SessionFactoryImplementor.class )
					.getRuntimeMetamodels()
					.getMappingMetamodel()
					.getEntityDescriptor( MainEntity.class );

			// use some specific lock options to trigger the creation of a loader with lock options
			LockOptions lockOptions = new LockOptions( LockMode.NONE );
			lockOptions.setTimeOut( 10 );

			MainEntity main = (MainEntity) entityPersister.
					load( this.mainId, null, lockOptions, (SharedSessionContractImplementor) em );

			assertNotNull( main.getSub() );
		} );
	}

	@Entity(name = "MainEntity")
	public static class MainEntity {

		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		@Fetch(org.hibernate.annotations.FetchMode.JOIN)
		private SubEntity sub;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public SubEntity getSub() {
			return sub;
		}

		public void setSub(SubEntity sub) {
			this.sub = sub;
		}
	}

	@Entity(name = "SubEntity")
	public static class SubEntity {

		@Id
		@GeneratedValue
		private Long id;

		public Long getId() {
			return id;
		}


		public void setId(Long id) {
			this.id = id;
		}
	}
}
