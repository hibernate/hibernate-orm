/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.modifiedflags;

import java.util.List;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.orm.test.envers.entities.collection.EnumSetEntity;
import org.hibernate.orm.test.envers.entities.collection.EnumSetEntity.E1;
import org.hibernate.orm.test.envers.entities.collection.EnumSetEntity.E2;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import static org.hibernate.orm.test.envers.integration.modifiedflags.AbstractModifiedFlagsEntityTest.queryForPropertyHasChanged;
import static org.hibernate.orm.test.envers.integration.modifiedflags.AbstractModifiedFlagsEntityTest.queryForPropertyHasNotChanged;
import static org.hibernate.orm.test.envers.tools.TestTools.extractRevisionNumbers;
import static org.hibernate.orm.test.envers.tools.TestTools.makeList;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
@Jpa(integrationSettings = @Setting(name = EnversSettings.GLOBAL_WITH_MODIFIED_FLAG, value = "true"),
		annotatedClasses = {EnumSetEntity.class})
public class HasChangedEnumSet extends AbstractModifiedFlagsEntityTest {
	private Integer sse1_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			EnumSetEntity sse1 = new EnumSetEntity();

			// Revision 1 (sse1: initially 1 element)
			em.getTransaction().begin();

			sse1.getEnums1().add( E1.X );
			sse1.getEnums2().add( E2.A );

			em.persist( sse1 );

			em.getTransaction().commit();

			// Revision 2 (sse1: adding 1 element/removing a non-existing element)
			em.getTransaction().begin();

			EnumSetEntity sse1Loaded = em.find( EnumSetEntity.class, sse1.getId() );

			sse1Loaded.getEnums1().add( E1.Y );
			sse1Loaded.getEnums2().remove( E2.B );

			em.getTransaction().commit();

			// Revision 3 (sse1: removing 1 element/adding an existing element)
			em.getTransaction().begin();

			sse1Loaded = em.find( EnumSetEntity.class, sse1.getId() );

			sse1Loaded.getEnums1().remove( E1.X );
			sse1Loaded.getEnums2().add( E2.A );

			em.getTransaction().commit();

			sse1_id = sse1.getId();
		} );
	}

	@Test
	public void testHasChanged(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			List list = queryForPropertyHasChanged(
					auditReader,
					EnumSetEntity.class, sse1_id,
					"enums1"
			);
			assertEquals( 3, list.size() );
			assertEquals( makeList( 1, 2, 3 ), extractRevisionNumbers( list ) );

			list = queryForPropertyHasChanged(
					auditReader,
					EnumSetEntity.class, sse1_id,
					"enums2"
			);
			assertEquals( 1, list.size() );
			assertEquals( makeList( 1 ), extractRevisionNumbers( list ) );

			list = queryForPropertyHasNotChanged(
					auditReader,
					EnumSetEntity.class, sse1_id,
					"enums1"
			);
			assertEquals( 0, list.size() );

			list = queryForPropertyHasNotChanged(
					auditReader,
					EnumSetEntity.class, sse1_id,
					"enums2"
			);
			assertEquals( 2, list.size() );
			assertEquals( makeList( 2, 3 ), extractRevisionNumbers( list ) );
		} );
	}
}
