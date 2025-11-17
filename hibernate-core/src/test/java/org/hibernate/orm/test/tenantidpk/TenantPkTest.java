/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tenantidpk;

import org.hibernate.binder.internal.TenantIdBinder;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryProducer;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hibernate.cfg.AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SessionFactory
@DomainModel(annotatedClasses = { Account.class, Client.class })
@ServiceRegistry(
		settings = {
				@Setting(name = JAKARTA_HBM2DDL_DATABASE_ACTION, value = "create-drop")
		}
)
public class TenantPkTest implements SessionFactoryProducer {

	private static final UUID mine = UUID.randomUUID();
	private static final UUID yours = UUID.randomUUID();

	UUID currentTenant;

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Override
	public SessionFactoryImplementor produceSessionFactory(MetadataImplementor model) {
		final SessionFactoryBuilder sessionFactoryBuilder = model.getSessionFactoryBuilder();
		sessionFactoryBuilder.applyCurrentTenantIdentifierResolver( new CurrentTenantIdentifierResolver<UUID>() {
			@Override
			public UUID resolveCurrentTenantIdentifier() {
				return currentTenant;
			}
			@Override
			public boolean validateExistingCurrentSessions() {
				return false;
			}
		} );
		return (SessionFactoryImplementor) sessionFactoryBuilder.build();
	}

	@Test
	public void test(SessionFactoryScope scope) {
		currentTenant = mine;
		Client client = new Client("Gavin");
		Account acc = new Account(client);
		scope.inTransaction( session -> {
			session.persist(client);
			session.persist(acc);
		} );
		scope.inTransaction( session -> {
			assertNotNull( session.createSelectionQuery("where id=?1", Account.class)
					.setParameter(1, acc.id)
					.getSingleResultOrNull() );
			assertEquals( 1, session.createQuery("from Account").getResultList().size() );
		} );
		assertEquals(mine, acc.tenantId);

		currentTenant = yours;
		scope.inTransaction( session -> {
			assertNull( session.createSelectionQuery("where id=?1", Account.class)
					.setParameter(1, acc.id)
					.getSingleResultOrNull() );
			assertEquals( 0, session.createQuery("from Account").getResultList().size() );
			session.disableFilter(TenantIdBinder.FILTER_NAME);
			assertNotNull( session.createSelectionQuery("where id=?1", Account.class)
					.setParameter(1, acc.id)
					.getSingleResultOrNull() );
			assertEquals( 1, session.createQuery("from Account").getResultList().size() );
		} );
	}

	@Test
	public void testErrorOnInsert(SessionFactoryScope scope) {
		currentTenant = mine;
		Client client = new Client("Gavin");
		Account acc = new Account(client);
		acc.tenantId = yours;
		scope.inTransaction( session -> {
			session.persist(client);
			session.persist(acc);
		} );
		assertEquals( mine, acc.tenantId );
		assertEquals( mine, client.tenantId );
	}
}
