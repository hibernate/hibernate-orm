package org.hibernate.event;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Version;

import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 * @author Nathan Xu
 * @author Tassilo Karge
 */
@TestForIssue( jiraKey = "HHH-14413" )
public class PreUpdateEventListenerVetoTest extends BaseCoreFunctionalTestCase {

	private static final Long EXAMPLE_ID_VALUE = 1L;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { ExampleEntity.class };
	}

	@Override
	protected void afterSessionFactoryBuilt() {
		super.afterSessionFactoryBuilt();
		EventListenerRegistry registry = sessionFactory().getServiceRegistry().getService( EventListenerRegistry.class );
		registry.appendListeners(
				EventType.PRE_UPDATE,
				event -> true
		);
	}

	@Before
	public void setUp() {
		doInHibernate( this::sessionFactory, session -> {
			ExampleEntity entity = new ExampleEntity();
			entity.id = EXAMPLE_ID_VALUE;
			entity.name = "old_name";
			session.save( entity );
		} );
	}

	@Test
	public void testVersionNotChangedWhenPreUpdateEventVetoed() {

		doInHibernate( this::sessionFactory, session -> {
			ExampleEntity entity = session.byId( ExampleEntity.class ).load( EXAMPLE_ID_VALUE );

			entity.name = "new_name";
			session.update( entity );

			final Long versionBeforeFlush = entity.version;

			session.flush();

			final Long versionAfterFlush = entity.version;

			assertEquals( "The entity version must not change when update is vetoed", versionBeforeFlush, versionAfterFlush );

		} );
	}

	@Entity(name = "ExampleEntity")
	public static class ExampleEntity {

		@Id
		Long id;

		String name;

		@Version
		Long version;

	}
}
