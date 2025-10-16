/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.modifiedflags;

import java.util.List;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.entities.components.relations.OneToManyComponent;
import org.hibernate.orm.test.envers.entities.components.relations.OneToManyComponentTestEntity;
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
		annotatedClasses = {OneToManyComponentTestEntity.class, StrTestEntity.class})
public class HasChangedOneToManyInComponent extends AbstractModifiedFlagsEntityTest {
	private Integer otmcte_id1;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			// Revision 1
			em.getTransaction().begin();

			StrTestEntity ste1 = new StrTestEntity();
			ste1.setStr( "str1" );

			StrTestEntity ste2 = new StrTestEntity();
			ste2.setStr( "str2" );

			em.persist( ste1 );
			em.persist( ste2 );

			em.getTransaction().commit();

			// Revision 2
			em.getTransaction().begin();

			OneToManyComponentTestEntity otmcte1 = new OneToManyComponentTestEntity( new OneToManyComponent( "data1" ) );
			otmcte1.getComp1().getEntities().add( ste1 );

			em.persist( otmcte1 );

			em.getTransaction().commit();

			// Revision 3
			em.getTransaction().begin();

			OneToManyComponentTestEntity otmcte1Ref = em.find( OneToManyComponentTestEntity.class, otmcte1.getId() );
			otmcte1Ref.getComp1().getEntities().add( ste2 );

			em.getTransaction().commit();

			otmcte_id1 = otmcte1.getId();
		} );
	}

	@Test
	public void testHasChangedId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			List list = AbstractModifiedFlagsEntityTest.queryForPropertyHasChanged(
					auditReader,
					OneToManyComponentTestEntity.class,
					otmcte_id1,
					"comp1"
			);
			assertEquals( 2, list.size() );
			assertEquals( makeList( 2, 3 ), extractRevisionNumbers( list ) );

			list = AbstractModifiedFlagsEntityTest.queryForPropertyHasNotChanged(
					auditReader,
					OneToManyComponentTestEntity.class,
					otmcte_id1,
					"comp1"
			);
			assertEquals( 0, list.size() );
		} );
	}
}
