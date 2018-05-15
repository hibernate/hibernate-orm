package org.hibernate.test.batchfetch;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertNotNull;

import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.annotations.Fetch;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.loader.BatchFetchStyle;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.junit.Test;

public class BatchingEntityLoaderInitializationWithNoLockModeTest extends BaseEntityManagerFunctionalTestCase {

	private Long mainId;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { MainEntity.class, SubEntity.class };
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected Map buildSettings() {
		Map settings = super.buildSettings();
		settings.put( AvailableSettings.BATCH_FETCH_STYLE, BatchFetchStyle.LEGACY );
		settings.put( AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, 5 );
		return settings;
	}

	@Test
	public void testJoin() {
		doInJPA( this::entityManagerFactory, em -> {
			SubEntity sub = new SubEntity();
			em.persist( sub );

			MainEntity main = new MainEntity();
			main.setSub( sub );
			em.persist( main );

			this.mainId = main.getId();
		});

		doInJPA( this::entityManagerFactory, em -> {
			EntityPersister entityPersister = ( (MetamodelImplementor) em.getMetamodel() )
					.entityPersister( MainEntity.class );

			// use some specific lock options to trigger the creation of a loader with lock options
			LockOptions lockOptions = new LockOptions( LockMode.NONE );
			lockOptions.setTimeOut( 10 );

			MainEntity main = (MainEntity) entityPersister.load( this.mainId, null, lockOptions,
					(SharedSessionContractImplementor) em );
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
