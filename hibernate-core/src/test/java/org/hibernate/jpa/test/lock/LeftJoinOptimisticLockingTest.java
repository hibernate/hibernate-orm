package org.hibernate.jpa.test.lock;

import javax.persistence.Tuple;

import java.util.UUID;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.query.NativeQuery;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Test;
import org.jboss.logging.Logger;

/**
 * This test demonstrates how a native query with LEFT join mixes up the version column when they have the same name
 * in the involved tables.
 * @author Fabio Souza
 */
public class LeftJoinOptimisticLockingTest extends BaseEntityManagerFunctionalTestCase {

	private static final Logger LOGGER = Logger.getLogger( LeftJoinOptimisticLockingTest.class );

	@Test
	public void test() {
		// Creating an app
		final App app = upsert(new App().setUuid(UUID.randomUUID()).setName("test"));

		// Creating installation
		final Installation installationBefore = upsert(
				new Installation()
						.setUuid(UUID.randomUUID())
						.setDetails("details")
						.setApp(app));


		// Creating an installation of that app and obtaining the opt_lock
		final long installOptLockBefore = installationBefore.getOptLock();

		// Bumping app "version" so it has a different value then the installation
		upsert(app.setName("changed"));
		// Bumping again so the difference is very obvious
		upsert(app.setName("changed2"));

		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			// Executing the left join query (app + installation)
			final NativeQuery nativeQuery = (NativeQuery) entityManager.createNativeQuery("select a.*, inst.* from App a " +
					"LEFT JOIN (select i.* from installation i) as inst " +
					"ON a.uuid = inst.app_uuid", Tuple.class);

			nativeQuery.addEntity("a", App.class);
			nativeQuery.addEntity("inst", Installation.class);
			nativeQuery.getResultList();

			// Finding the installation (no join). This should not cause any changes in the opt_lock
			final Installation installationAfter = entityManager.find(Installation.class, installationBefore.getUuid());
			final long installationOptLockAfter = installationAfter.getOptLock();

			// There were no changes so far, therefore, the opt_lock should be the same
			LOGGER.info("Installation opt lock before: " + installOptLockBefore);
			LOGGER.info("Installation opt lock after: " + installationOptLockAfter);
			if (installationBefore != installationAfter) {
				LOGGER.error("Installation opt lock obtained is different from after executing the LEFT join");
			}


			// Changing and saving the installation
			// Failing means that the left join query mixed up the opt_lock columns
			entityManager.persist(installationAfter.setDetails("change"));

		} );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { App.class, Installation.class };
	}

	private App upsert(final App app) {
		return TransactionUtil.doInJPA( this::entityManagerFactory, em -> {
			final App existingApp = em.find(App.class, app.getUuid());
			final App mergedApp = existingApp != null ? existingApp.setName(app.getName()) : app;
			em.persist(mergedApp);
			return mergedApp;
		} );
	}

	private Installation upsert(final Installation installation) {
		return TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			final Installation existingEntity = entityManager.find(Installation.class, installation.getUuid());
			final Installation mergedEntity = existingEntity != null ? existingEntity.setDetails(installation.getDetails()) : installation;
			entityManager.persist(mergedEntity);
			return mergedEntity;
		} );
	}

}
