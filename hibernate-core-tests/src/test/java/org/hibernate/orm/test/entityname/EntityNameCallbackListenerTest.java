package org.hibernate.orm.test.entityname;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.persistence.PreUpdate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A class mapped with an explicit entity name is also registered
 * under its class name, but must still be a single entity type.
 */
@JiraKey("HHH-21118")
@DomainModel(xmlMappings = "org/hibernate/orm/test/entityname/Customer.hbm.xml")
@SessionFactory
public class EntityNameCallbackListenerTest {

	@Test
	public void preUpdateListenerFiresOnce(SessionFactoryScope scope) {
		final var calls = new AtomicInteger();
		final var registration =
				scope.getSessionFactory()
						.addListener( Customer.class, PreUpdate.class,
								customer -> calls.incrementAndGet() );
		try {
			final long id = scope.fromTransaction( session -> {
				final var customer = new Customer();
				customer.setCode( "c1" );
				session.persist( "Customer", customer );
				return customer.getId();
			} );
			scope.inTransaction( session -> {
				final var customer = (Customer) session.find( "Customer", id );
				customer.setCode( "c2" );
			} );
			assertEquals( 1, calls.get() );
		}
		finally {
			registration.cancel();
		}
	}

	@Test
	public void classNameIsNotAnEntityName(SessionFactoryScope scope) {
		assertArrayEquals( new String[] {"Customer"},
				scope.getSessionFactory().getMappingMetamodel().getAllEntityNames() );
	}
}
