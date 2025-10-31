/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.modifiedflags;

import java.util.List;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.integration.collection.mapkey.IdMapKeyEntity;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import static org.hibernate.orm.test.envers.tools.TestTools.extractRevisionNumbers;
import static org.hibernate.orm.test.envers.tools.TestTools.makeList;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
@Jpa(integrationSettings = @Setting(name = EnversSettings.GLOBAL_WITH_MODIFIED_FLAG, value = "true"),
		annotatedClasses = {IdMapKeyEntity.class, StrTestEntity.class})
public class HasChangedIdMapKey extends AbstractModifiedFlagsEntityTest {
	private Integer imke_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			IdMapKeyEntity imke = new IdMapKeyEntity();

			// Revision 1 (intialy 1 mapping)
			em.getTransaction().begin();

			StrTestEntity ste1 = new StrTestEntity( "x" );
			StrTestEntity ste2 = new StrTestEntity( "y" );

			em.persist( ste1 );
			em.persist( ste2 );

			imke.getIdmap().put( ste1.getId(), ste1 );

			em.persist( imke );

			em.getTransaction().commit();

			// Revision 2 (sse1: adding 1 mapping)
			em.getTransaction().begin();

			StrTestEntity ste2Ref = em.find( StrTestEntity.class, ste2.getId() );
			IdMapKeyEntity imkeRef = em.find( IdMapKeyEntity.class, imke.getId() );

			imkeRef.getIdmap().put( ste2Ref.getId(), ste2Ref );

			em.getTransaction().commit();

			imke_id = imke.getId();
		} );
	}

	@Test
	public void testHasChanged(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			List list = AbstractModifiedFlagsEntityTest.queryForPropertyHasChanged(
					auditReader,
					IdMapKeyEntity.class,
					imke_id,
					"idmap"
			);
			assertEquals( 2, list.size() );
			assertEquals( makeList( 1, 2 ), extractRevisionNumbers( list ) );

			list = AbstractModifiedFlagsEntityTest.queryForPropertyHasNotChanged(
					auditReader,
					IdMapKeyEntity.class,
					imke_id,
					"idmap"
			);
			assertEquals( 0, list.size() );
		} );
	}
}
