/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.entity;


import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author Emmanuel Bernard
 */
@DomainModel(
		annotatedClasses = {Address.class,
				WashingMachine.class

		}
)
@SessionFactory
public class PropertyDefaultMappingsTest {
	@AfterEach
	public void afterEach(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}


	@Test
	public void testSerializableObject(SessionFactoryScope scope) {
		Address a = new Address();
		scope.inTransaction(
				session -> {
					Country c = new Country();
					c.setName( "France" );
					a.setCity( "Paris" );
					a.setCountry( c );
					session.persist( a );
				}
		);

		scope.inTransaction(
				session -> {
					Address reloadedAddress = session.find( Address.class, a.getId() );
					assertThat( reloadedAddress ).isNotNull();
					assertThat( reloadedAddress.getCountry() ).isNotNull();
					assertThat( reloadedAddress.getCountry().getName() ).isEqualTo( a.getCountry().getName() );
				}
		);
	}

	@Test
	public void testTransientField(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					WashingMachine wm = new WashingMachine();
					wm.setActive( true );
					session.persist( wm );
					session.getTransaction().commit();
					session.clear();
					session.beginTransaction();
					wm = session.find( WashingMachine.class, wm.getId() );
					assertThat( wm.isActive() ).isFalse();
					session.remove( wm );
				}
		);
	}
}
