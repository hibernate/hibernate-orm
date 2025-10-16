/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.modifiedflags;

import java.util.List;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.orm.test.envers.entities.collection.StringMapEntity;
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
		annotatedClasses = {StringMapEntity.class})
public class HasChangedStringMap extends AbstractModifiedFlagsEntityTest {
	private Integer sme1_id;
	private Integer sme2_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		StringMapEntity sme1 = new StringMapEntity();
		StringMapEntity sme2 = new StringMapEntity();

		// Revision 1 (sme1: initialy empty, sme2: initialy 1 mapping)
		scope.inEntityManager( em -> {
			em.getTransaction().begin();

			sme2.getStrings().put( "1", "a" );

			em.persist( sme1 );
			em.persist( sme2 );
			sme1_id = sme1.getId();
			sme2_id = sme2.getId();

			em.getTransaction().commit();
		} );

		// Revision 2 (sme1: adding 2 mappings, sme2: no changes)
		scope.inEntityManager( em -> {
			em.getTransaction().begin();

			StringMapEntity sme1Loaded = em.find( StringMapEntity.class, sme1_id );

			sme1Loaded.getStrings().put( "1", "a" );
			sme1Loaded.getStrings().put( "2", "b" );

			em.getTransaction().commit();
		} );

		// Revision 3 (sme1: removing an existing mapping, sme2: replacing a value)
		scope.inEntityManager( em -> {
			em.getTransaction().begin();

			StringMapEntity sme1Loaded = em.find( StringMapEntity.class, sme1_id );
			StringMapEntity sme2Loaded = em.find( StringMapEntity.class, sme2_id );

			sme1Loaded.getStrings().remove( "1" );
			sme2Loaded.getStrings().put( "1", "b" );

			em.getTransaction().commit();
		} );

		// No revision (sme1: removing a non-existing mapping, sme2: replacing with the same value)
		scope.inEntityManager( em -> {
			em.getTransaction().begin();

			StringMapEntity sme1Loaded = em.find( StringMapEntity.class, sme1_id );
			StringMapEntity sme2Loaded = em.find( StringMapEntity.class, sme2_id );

			sme1Loaded.getStrings().remove( "3" );
			sme2Loaded.getStrings().put( "1", "b" );

			em.getTransaction().commit();
		} );
	}

	@Test
	public void testHasChanged(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			List list = queryForPropertyHasChanged(
					auditReader,
					StringMapEntity.class, sme1_id,
					"strings"
			);
			assertEquals( 3, list.size() );
			assertEquals( makeList( 1, 2, 3 ), extractRevisionNumbers( list ) );

			list = queryForPropertyHasChanged(
					auditReader,
					StringMapEntity.class, sme2_id,
					"strings"
			);
			assertEquals( 2, list.size() );
			assertEquals( makeList( 1, 3 ), extractRevisionNumbers( list ) );

			list = queryForPropertyHasNotChanged(
					auditReader,
					StringMapEntity.class, sme1_id,
					"strings"
			);
			assertEquals( 0, list.size() );

			list = queryForPropertyHasNotChanged(
					auditReader,
					StringMapEntity.class, sme2_id,
					"strings"
			);
			assertEquals( 0, list.size() ); // in rev 2 there was no version generated for sme2_id
		} );
	}
}
