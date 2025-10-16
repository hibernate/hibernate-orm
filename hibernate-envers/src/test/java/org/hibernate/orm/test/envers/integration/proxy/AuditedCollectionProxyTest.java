/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.proxy;

import org.hibernate.orm.test.envers.entities.onetomany.ListRefEdEntity;
import org.hibernate.orm.test.envers.entities.onetomany.ListRefIngEntity;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Test case for HHH-5750: Proxied objects lose the temporary session used to
 * initialize them.
 *
 * @author Erik-Berndt Scheper
 */
@Jpa(annotatedClasses = {ListRefEdEntity.class, ListRefIngEntity.class})
@EnversTest
public class AuditedCollectionProxyTest {

	Integer id_ListRefEdEntity1;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		ListRefEdEntity listReferencedEntity1 = new ListRefEdEntity(
				Integer.valueOf( 1 ), "str1"
		);
		ListRefIngEntity refingEntity1 = new ListRefIngEntity(
				Integer.valueOf( 1 ), "refing1", listReferencedEntity1
		);

		// Revision 1
		scope.inTransaction( em -> {
			em.persist( listReferencedEntity1 );
			em.persist( refingEntity1 );
		} );

		id_ListRefEdEntity1 = listReferencedEntity1.getId();

		// Revision 2
		ListRefIngEntity refingEntity2 = new ListRefIngEntity(
				Integer.valueOf( 2 ), "refing2", listReferencedEntity1
		);

		scope.inTransaction( em -> {
			em.persist( refingEntity2 );
		} );
	}

	@Test
	public void testProxyIdentifier(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			ListRefEdEntity listReferencedEntity1 = em.getReference(
					ListRefEdEntity.class, id_ListRefEdEntity1
			);

			assertInstanceOf( HibernateProxy.class, listReferencedEntity1 );

			// Revision 3
			ListRefIngEntity refingEntity3 = new ListRefIngEntity(
					Integer.valueOf( 3 ), "refing3", listReferencedEntity1
			);

			em.persist( refingEntity3 );

			listReferencedEntity1.getReffering().size();
		} );
	}

}
