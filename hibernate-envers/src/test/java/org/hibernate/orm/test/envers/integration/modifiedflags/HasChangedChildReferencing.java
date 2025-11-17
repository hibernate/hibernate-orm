/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.modifiedflags;

import java.util.List;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.orm.test.envers.integration.inheritance.joined.childrelation.ChildIngEntity;
import org.hibernate.orm.test.envers.integration.inheritance.joined.childrelation.ParentNotIngEntity;
import org.hibernate.orm.test.envers.integration.inheritance.joined.childrelation.ReferencedEntity;
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
		annotatedClasses = {ChildIngEntity.class, ParentNotIngEntity.class, ReferencedEntity.class})
public class HasChangedChildReferencing extends AbstractModifiedFlagsEntityTest {
	private Integer re_id1;
	private Integer re_id2;
	private Integer c_id;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		re_id1 = 1;
		re_id2 = 10;
		c_id = 100;

		// Rev 1
		scope.inEntityManager( em -> {
			em.getTransaction().begin();

			ReferencedEntity re1 = new ReferencedEntity( re_id1 );
			em.persist( re1 );

			ReferencedEntity re2 = new ReferencedEntity( re_id2 );
			em.persist( re2 );

			em.getTransaction().commit();
		} );

		// Rev 2
		scope.inEntityManager( em -> {
			em.getTransaction().begin();

			ReferencedEntity re1 = em.find( ReferencedEntity.class, re_id1 );

			ChildIngEntity cie = new ChildIngEntity( c_id, "y", 1l );
			cie.setReferenced( re1 );
			em.persist( cie );

			em.getTransaction().commit();
		} );

		// Rev 3
		scope.inEntityManager( em -> {
			em.getTransaction().begin();

			ReferencedEntity re2 = em.find( ReferencedEntity.class, re_id2 );
			ChildIngEntity cie = em.find( ChildIngEntity.class, c_id );

			cie.setReferenced( re2 );

			em.getTransaction().commit();
		} );
	}

	@Test
	public void testReferencedEntityHasChanged(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			List list = queryForPropertyHasChanged( auditReader, ReferencedEntity.class, re_id1, "referencing" );
			assertEquals( 2, list.size() );
			assertEquals( makeList( 2, 3 ), extractRevisionNumbers( list ) );

			list = queryForPropertyHasNotChanged( auditReader, ReferencedEntity.class, re_id1, "referencing" );
			assertEquals( 1, list.size() ); // initially referencing collection is null
			assertEquals( makeList( 1 ), extractRevisionNumbers( list ) );

			list = queryForPropertyHasChanged( auditReader, ReferencedEntity.class, re_id2, "referencing" );
			assertEquals( 1, list.size() );
			assertEquals( makeList( 3 ), extractRevisionNumbers( list ) );
		} );
	}

}
