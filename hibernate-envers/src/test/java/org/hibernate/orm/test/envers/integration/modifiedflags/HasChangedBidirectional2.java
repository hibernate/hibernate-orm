/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.modifiedflags;

import java.util.List;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.orm.test.envers.integration.onetoone.bidirectional.BiRefEdEntity;
import org.hibernate.orm.test.envers.integration.onetoone.bidirectional.BiRefIngEntity;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hibernate.orm.test.envers.tools.TestTools.extractRevisionNumbers;
import static org.hibernate.orm.test.envers.tools.TestTools.makeList;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
@Jpa(integrationSettings = @Setting(name = EnversSettings.GLOBAL_WITH_MODIFIED_FLAG, value = "true"),
		annotatedClasses = {BiRefEdEntity.class, BiRefIngEntity.class})
public class HasChangedBidirectional2 extends AbstractModifiedFlagsEntityTest {
	private Integer ed1_id;
	private Integer ed2_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		BiRefEdEntity ed1 = new BiRefEdEntity( 1, "data_ed_1" );
		BiRefEdEntity ed2 = new BiRefEdEntity( 2, "data_ed_2" );

		BiRefIngEntity ing1 = new BiRefIngEntity( 3, "data_ing_1" );
		BiRefIngEntity ing2 = new BiRefIngEntity( 4, "data_ing_2" );

		// Revision 1
		scope.inEntityManager( em -> {
			em.getTransaction().begin();

			em.persist( ed1 );
			em.persist( ed2 );

			em.getTransaction().commit();
		} );

		// Revision 2
		scope.inEntityManager( em -> {
			em.getTransaction().begin();

			BiRefEdEntity ed1Ref = em.find( BiRefEdEntity.class, ed1.getId() );

			ing1.setReference( ed1Ref );

			em.persist( ing1 );
			em.persist( ing2 );

			em.getTransaction().commit();
		} );

		// Revision 3
		scope.inEntityManager( em -> {
			em.getTransaction().begin();

			BiRefEdEntity ed1Ref = em.find( BiRefEdEntity.class, ed1.getId() );
			BiRefIngEntity ing1Ref = em.find( BiRefIngEntity.class, ing1.getId() );
			BiRefIngEntity ing2Ref = em.find( BiRefIngEntity.class, ing2.getId() );

			ing1Ref.setReference( null );
			ing2Ref.setReference( ed1Ref );

			em.getTransaction().commit();
		} );

		// Revision 4
		scope.inEntityManager( em -> {
			em.getTransaction().begin();

			BiRefEdEntity ed2Ref = em.find( BiRefEdEntity.class, ed2.getId() );
			BiRefIngEntity ing1Ref = em.find( BiRefIngEntity.class, ing1.getId() );
			BiRefIngEntity ing2Ref = em.find( BiRefIngEntity.class, ing2.getId() );

			ing1Ref.setReference( ed2Ref );
			ing2Ref.setReference( null );

			em.getTransaction().commit();
		} );

		ed1_id = ed1.getId();
		ed2_id = ed2.getId();
	}

	@Test
	public void testHasChanged(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			List list = queryForPropertyHasChanged(
					auditReader, BiRefEdEntity.class, ed1_id,
					"referencing"
			);
			assertEquals( 3, list.size() );
			assertEquals( makeList( 2, 3, 4 ), extractRevisionNumbers( list ) );

			list = queryForPropertyHasChanged(
					auditReader, BiRefEdEntity.class, ed2_id,
					"referencing"
			);
			assertEquals( 1, list.size() );
			assertEquals( makeList( 4 ), extractRevisionNumbers( list ) );
		} );
	}
}
