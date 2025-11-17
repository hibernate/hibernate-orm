/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.modifiedflags;

import java.util.List;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.orm.test.envers.entities.collection.StringSetEntity;
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
		annotatedClasses = {StringSetEntity.class})
public class HasChangedStringSet extends AbstractModifiedFlagsEntityTest {
	private Integer sse1_id;
	private Integer sse2_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		StringSetEntity sse1 = new StringSetEntity();
		StringSetEntity sse2 = new StringSetEntity();

		// Revision 1 (sse1: initialy empty, sse2: initialy 2 elements)
		scope.inEntityManager( em -> {
			em.getTransaction().begin();

			sse2.getStrings().add( "sse2_string1" );
			sse2.getStrings().add( "sse2_string2" );

			em.persist( sse1 );
			em.persist( sse2 );
			sse1_id = sse1.getId();
			sse2_id = sse2.getId();

			em.getTransaction().commit();
		} );

		// Revision 2 (sse1: adding 2 elements, sse2: adding an existing element)
		scope.inEntityManager( em -> {
			em.getTransaction().begin();

			StringSetEntity sse1Loaded = em.find( StringSetEntity.class, sse1_id );
			StringSetEntity sse2Loaded = em.find( StringSetEntity.class, sse2_id );

			sse1Loaded.getStrings().add( "sse1_string1" );
			sse1Loaded.getStrings().add( "sse1_string2" );

			sse2Loaded.getStrings().add( "sse2_string1" );

			em.getTransaction().commit();
		} );

		// Revision 3 (sse1: removing a non-existing element, sse2: removing one element)
		scope.inEntityManager( em -> {
			em.getTransaction().begin();

			StringSetEntity sse1Loaded = em.find( StringSetEntity.class, sse1_id );
			StringSetEntity sse2Loaded = em.find( StringSetEntity.class, sse2_id );

			sse1Loaded.getStrings().remove( "sse1_string3" );
			sse2Loaded.getStrings().remove( "sse2_string1" );

			em.getTransaction().commit();
		} );
	}

	@Test
	public void testHasChanged(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			List list = queryForPropertyHasChanged(
					auditReader,
					StringSetEntity.class, sse1_id,
					"strings"
			);
			assertEquals( 2, list.size() );
			assertEquals( makeList( 1, 2 ), extractRevisionNumbers( list ) );

			list = queryForPropertyHasChanged(
					auditReader,
					StringSetEntity.class, sse2_id,
					"strings"
			);
			assertEquals( 2, list.size() );
			assertEquals( makeList( 1, 3 ), extractRevisionNumbers( list ) );

			list = queryForPropertyHasNotChanged(
					auditReader,
					StringSetEntity.class, sse1_id,
					"strings"
			);
			assertEquals( 0, list.size() );

			list = queryForPropertyHasNotChanged(
					auditReader,
					StringSetEntity.class, sse2_id,
					"strings"
			);
			assertEquals( 0, list.size() );
		} );
	}
}
