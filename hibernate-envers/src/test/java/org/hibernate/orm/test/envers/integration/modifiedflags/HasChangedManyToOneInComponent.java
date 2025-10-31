/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.modifiedflags;

import java.util.List;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.entities.components.relations.ManyToOneComponent;
import org.hibernate.orm.test.envers.entities.components.relations.ManyToOneComponentTestEntity;
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
		annotatedClasses = {ManyToOneComponentTestEntity.class, StrTestEntity.class})
public class HasChangedManyToOneInComponent extends AbstractModifiedFlagsEntityTest {
	private Integer mtocte_id1;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1
		scope.inEntityManager( em -> {
			em.getTransaction().begin();

			StrTestEntity ste1 = new StrTestEntity();
			ste1.setStr( "str1" );

			StrTestEntity ste2 = new StrTestEntity();
			ste2.setStr( "str2" );

			em.persist( ste1 );
			em.persist( ste2 );

			em.getTransaction().commit();
		} );

		// Revision 2
		scope.inEntityManager( em -> {
			em.getTransaction().begin();

			StrTestEntity ste1 = em.createQuery( "from StrTestEntity where str = 'str1'", StrTestEntity.class )
					.getSingleResult();
			ManyToOneComponentTestEntity mtocte1 = new ManyToOneComponentTestEntity(
					new ManyToOneComponent(
							ste1,
							"data1"
					)
			);

			em.persist( mtocte1 );
			mtocte_id1 = mtocte1.getId();

			em.getTransaction().commit();
		} );

		// Revision 3
		scope.inEntityManager( em -> {
			em.getTransaction().begin();

			ManyToOneComponentTestEntity mtocte1 = em.find( ManyToOneComponentTestEntity.class, mtocte_id1 );
			StrTestEntity ste2 = em.createQuery( "from StrTestEntity where str = 'str2'", StrTestEntity.class )
					.getSingleResult();
			mtocte1.getComp1().setEntity( ste2 );

			em.getTransaction().commit();
		} );
	}

	@Test
	public void testHasChangedId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			List list = queryForPropertyHasChanged(
					auditReader,
					ManyToOneComponentTestEntity.class,
					mtocte_id1, "comp1"
			);
			assertEquals( 2, list.size() );
			assertEquals( makeList( 2, 3 ), extractRevisionNumbers( list ) );

			list = queryForPropertyHasNotChanged(
					auditReader,
					ManyToOneComponentTestEntity.class,
					mtocte_id1, "comp1"
			);
			assertEquals( 0, list.size() );
		} );
	}

}
