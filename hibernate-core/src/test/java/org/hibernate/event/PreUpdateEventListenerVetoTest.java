/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Nathan Xu
 * @author Tassilo Karge
 */
@JiraKey(value = "HHH-14413")
public class PreUpdateEventListenerVetoTest extends BaseSessionFactoryFunctionalTest {

	private static final Long EXAMPLE_ID_VALUE = 1L;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { ExampleEntity.class };
	}

	@Override
	protected void sessionFactoryBuilt(SessionFactoryImplementor factory) {
		factory.getServiceRegistry().requireService( EventListenerRegistry.class )
				.appendListeners( EventType.PRE_UPDATE, event -> true );
	}

	@BeforeEach
	public void setUp() {
		inTransaction( session -> {
			ExampleEntity entity = new ExampleEntity();
			entity.id = EXAMPLE_ID_VALUE;
			entity.name = "old_name";
			session.persist( entity );
		} );
	}

	@Test
	public void testVersionNotChangedWhenPreUpdateEventVetoed() {

		inTransaction( session -> {
			ExampleEntity entity = session.byId( ExampleEntity.class ).load( EXAMPLE_ID_VALUE );

			entity.name = "new_name";
			entity = session.merge( entity );

			final Long versionBeforeFlush = entity.version;

			session.flush();

			final Long versionAfterFlush = entity.version;

			assertEquals(
					versionBeforeFlush,
					versionAfterFlush,
					"The entity version must not change when update is vetoed"
			);

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
