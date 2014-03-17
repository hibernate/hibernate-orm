package org.hibernate.test.events;

import org.hibernate.IrrelevantEntity;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostCommitDeleteEventListener;
import org.hibernate.event.spi.PostCommitInsertEventListener;
import org.hibernate.event.spi.PostCommitUpdateEventListener;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * Test to ensure that the existing post commit behavior when using plain PostXEventListeners fire on both success and failure.
 * 
 * @author ShawnClowater
 */
public class PostCommitListenerTest extends BaseCoreFunctionalTestCase {
	private PostInsertEventListener postCommitInsertEventListener = new TestPostCommitInsertEventListener();
	private PostDeleteEventListener postCommitDeleteEventListener = new TestPostCommitDeleteEventListener();
	private PostUpdateEventListener postCommitUpdateEventListener = new TestPostCommitUpdateEventListener();

	@Override
	protected void prepareTest() throws Exception {
		((TestPostCommitInsertEventListener) postCommitInsertEventListener).success = 0;
		((TestPostCommitInsertEventListener) postCommitInsertEventListener).failed = 0;
		((TestPostCommitDeleteEventListener) postCommitDeleteEventListener).success = 0;
		((TestPostCommitDeleteEventListener) postCommitDeleteEventListener).failed = 0;
		((TestPostCommitUpdateEventListener) postCommitUpdateEventListener).sucess = 0;
		((TestPostCommitUpdateEventListener) postCommitUpdateEventListener).failed = 0;
	}

	@Override
	protected void prepareBootstrapRegistryBuilder(BootstrapServiceRegistryBuilder builder) {
		super.prepareBootstrapRegistryBuilder( builder );
		builder.with(
				new Integrator() {

					@Override
					public void integrate(
							Configuration configuration,
							SessionFactoryImplementor sessionFactory,
							SessionFactoryServiceRegistry serviceRegistry) {
						integrate( serviceRegistry );
					}

					@Override
					public void integrate(
							MetadataImplementor metadata,
							SessionFactoryImplementor sessionFactory,
							SessionFactoryServiceRegistry serviceRegistry) {
						integrate( serviceRegistry );
					}

					private void integrate(SessionFactoryServiceRegistry serviceRegistry) {
						serviceRegistry.getService( EventListenerRegistry.class ).getEventListenerGroup(
								EventType.POST_COMMIT_DELETE
						).appendListener( postCommitDeleteEventListener );
						serviceRegistry.getService( EventListenerRegistry.class ).getEventListenerGroup(
								EventType.POST_COMMIT_UPDATE
						).appendListener( postCommitUpdateEventListener );
						serviceRegistry.getService( EventListenerRegistry.class ).getEventListenerGroup(
								EventType.POST_COMMIT_INSERT
						).appendListener( postCommitInsertEventListener );
					}

					@Override
					public void disintegrate(
							SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
					}
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-1582")
	public void testPostCommitInsertListenerSuccess() {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		IrrelevantEntity irrelevantEntity = new IrrelevantEntity();
		irrelevantEntity.setName( "Irrelevant" );

		session.save( irrelevantEntity );
		session.flush();
		transaction.commit();
		session.close();

		Assert.assertEquals( 1, ((TestPostCommitInsertEventListener) postCommitInsertEventListener).success );
		Assert.assertEquals( 0, ((TestPostCommitInsertEventListener) postCommitInsertEventListener).failed );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-1582")
	public void testPostCommitInsertListenerRollback() {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		IrrelevantEntity irrelevantEntity = new IrrelevantEntity();
		irrelevantEntity.setName( "Irrelevant" );

		session.save( irrelevantEntity );
		session.flush();
		transaction.rollback();
		session.close();

		Assert.assertEquals( 0, ((TestPostCommitInsertEventListener) postCommitInsertEventListener).success );
		Assert.assertEquals( 1, ((TestPostCommitInsertEventListener) postCommitInsertEventListener).failed );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-1582")
	public void testPostCommitUpdateListenerSuccess() {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		IrrelevantEntity irrelevantEntity = new IrrelevantEntity();
		irrelevantEntity.setName( "Irrelevant" );

		session.save( irrelevantEntity );
		session.flush();
		transaction.commit();

		session = openSession();
		transaction = session.beginTransaction();
		irrelevantEntity.setName( "Irrelevant 2" );
		session.update( irrelevantEntity );
		session.flush();
		transaction.commit();

		session.close();

		Assert.assertEquals( 1, ((TestPostCommitUpdateEventListener) postCommitUpdateEventListener).sucess );
		Assert.assertEquals( 0, ((TestPostCommitUpdateEventListener) postCommitUpdateEventListener).failed );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-1582")
	public void testPostCommitUpdateListenerRollback() {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		IrrelevantEntity irrelevantEntity = new IrrelevantEntity();
		irrelevantEntity.setName( "Irrelevant" );

		session.save( irrelevantEntity );
		session.flush();
		transaction.commit();
		session.close();

		session = openSession();
		transaction = session.beginTransaction();
		irrelevantEntity.setName( "Irrelevant 2" );
		session.update( irrelevantEntity );
		session.flush();
		transaction.rollback();

		session.close();

		Assert.assertEquals( 0, ((TestPostCommitUpdateEventListener) postCommitUpdateEventListener).sucess );
		Assert.assertEquals( 1, ((TestPostCommitUpdateEventListener) postCommitUpdateEventListener).failed );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-1582")
	public void testPostCommitDeleteListenerSuccess() {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		IrrelevantEntity irrelevantEntity = new IrrelevantEntity();
		irrelevantEntity.setName( "Irrelevant" );

		session.save( irrelevantEntity );
		session.flush();
		transaction.commit();
		session.close();

		session = openSession();
		transaction = session.beginTransaction();
		session.delete( irrelevantEntity );
		session.flush();
		transaction.commit();

		session.close();

		Assert.assertEquals( 1, ((TestPostCommitDeleteEventListener) postCommitDeleteEventListener).success );
		Assert.assertEquals( 0, ((TestPostCommitDeleteEventListener) postCommitDeleteEventListener).failed );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-1582")
	public void testPostCommitDeleteListenerRollback() {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		IrrelevantEntity irrelevantEntity = new IrrelevantEntity();
		irrelevantEntity.setName( "Irrelevant" );

		session.save( irrelevantEntity );
		session.flush();
		transaction.commit();
		session.close();

		session = openSession();
		transaction = session.beginTransaction();
		session.delete( irrelevantEntity );
		session.flush();
		transaction.rollback();

		session.close();

		Assert.assertEquals( 0, ((TestPostCommitDeleteEventListener) postCommitDeleteEventListener).success );
		Assert.assertEquals( 1, ((TestPostCommitDeleteEventListener) postCommitDeleteEventListener).failed );
	}

	private class TestPostCommitDeleteEventListener implements PostCommitDeleteEventListener {
		int success;
		int failed;

		@Override
		public void onPostDelete(PostDeleteEvent event) {
			success++;
		}

		@Override
		public void onPostDeleteCommitFailed(PostDeleteEvent event) {
			failed++;
		}

		@Override
		public boolean requiresPostCommitHanding(EntityPersister persister) {
			return true;
		}
	}

	private class TestPostCommitUpdateEventListener implements PostCommitUpdateEventListener {
		int sucess;
		int failed;

		@Override
		public void onPostUpdate(PostUpdateEvent event) {
			sucess++;
		}

		@Override
		public void onPostUpdateCommitFailed(PostUpdateEvent event) {
			failed++;
		}

		@Override
		public boolean requiresPostCommitHanding(EntityPersister persister) {
			return true;
		}
	}

	private class TestPostCommitInsertEventListener implements PostCommitInsertEventListener {
		int success;
		int failed;

		@Override
		public void onPostInsert(PostInsertEvent event) {
			success++;
		}

		@Override
		public void onPostInsertCommitFailed(PostInsertEvent event) {
			failed++;
		}

		@Override
		public boolean requiresPostCommitHanding(EntityPersister persister) {
			return true;
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {IrrelevantEntity.class};
	}
}
