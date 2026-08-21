/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.modifiedflags;

import java.util.List;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.orm.test.envers.entities.components.Component1;
import org.hibernate.orm.test.envers.entities.components.Component2;
import org.hibernate.orm.test.envers.entities.components.ComponentTestEntity;
import org.hibernate.orm.test.envers.integration.collection.mapkey.ComponentMapKeyEntity;
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
		annotatedClasses = {ComponentMapKeyEntity.class, ComponentTestEntity.class})
public class HasChangedComponentMapKey extends AbstractModifiedFlagsEntityTest {
	private Integer cmke_id;

	private Integer cte1_id;
	private Integer cte2_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			ComponentMapKeyEntity imke = new ComponentMapKeyEntity();

			// Revision 1 (initially 1 mapping)
			em.getTransaction().begin();

			ComponentTestEntity cte1 = new ComponentTestEntity(
					new Component1( "x1", "y2" ), new Component2(
					"a1",
					"b2"
			)
			);
			ComponentTestEntity cte2 = new ComponentTestEntity(
					new Component1( "x1", "y2" ), new Component2(
					"a1",
					"b2"
			)
			);

			em.persist( cte1 );
			em.persist( cte2 );

			imke.getIdmap().put( cte1.getComp1(), cte1 );

			em.persist( imke );

			em.getTransaction().commit();

			// Revision 2 (sse1: adding 1 mapping)
			em.getTransaction().begin();

			ComponentTestEntity cte2Loaded = em.find( ComponentTestEntity.class, cte2.getId() );
			ComponentMapKeyEntity imkeLoaded = em.find( ComponentMapKeyEntity.class, imke.getId() );

			imkeLoaded.getIdmap().put( cte2Loaded.getComp1(), cte2Loaded );

			em.getTransaction().commit();

			cmke_id = imke.getId();
			cte1_id = cte1.getId();
			cte2_id = cte2.getId();
		} );
	}

	@Test
	public void testHasChangedMapEntity(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			List list = queryForPropertyHasChanged( auditReader, ComponentMapKeyEntity.class, cmke_id, "idmap" );
			assertEquals( 2, list.size() );
			assertEquals( makeList( 1, 2 ), extractRevisionNumbers( list ) );

			list = queryForPropertyHasNotChanged(
					auditReader,
					ComponentMapKeyEntity.class,
					cmke_id, "idmap"
			);
			assertEquals( 0, list.size() );
		} );
	}

	@Test
	public void testHasChangedComponentEntity(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			List list = queryForPropertyHasChanged(
					auditReader,
					ComponentTestEntity.class,
					cte1_id, "comp1"
			);
			assertEquals( 1, list.size() );
			assertEquals( makeList( 1 ), extractRevisionNumbers( list ) );

			list = queryForPropertyHasNotChanged(
					auditReader,
					ComponentTestEntity.class, cte1_id,
					"comp1"
			);
			assertEquals( 0, list.size() );

			list = queryForPropertyHasChanged( auditReader, ComponentTestEntity.class, cte2_id, "comp1" );
			assertEquals( 1, list.size() );
			assertEquals( makeList( 1 ), extractRevisionNumbers( list ) );

			list = queryForPropertyHasNotChanged( auditReader, ComponentTestEntity.class, cte2_id, "comp1" );
			assertEquals( 0, list.size() );
		} );
	}
}
