/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.modifiedflags;

import java.util.List;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.orm.test.envers.integration.inheritance.joined.ChildEntity;
import org.hibernate.orm.test.envers.integration.inheritance.joined.ParentEntity;
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
		annotatedClasses = {ChildEntity.class, ParentEntity.class})
public class HasChangedChildAuditing extends AbstractModifiedFlagsEntityTest {
	private Integer id1;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		id1 = 1;

		// Rev 1
		scope.inEntityManager( em -> {
			em.getTransaction().begin();
			ChildEntity ce = new ChildEntity( id1, "x", 1l );
			em.persist( ce );
			em.getTransaction().commit();
		} );

		// Rev 2
		scope.inEntityManager( em -> {
			em.getTransaction().begin();
			ChildEntity ce = em.find( ChildEntity.class, id1 );
			ce.setData( "y" );
			ce.setNumVal( 2l );
			em.getTransaction().commit();
		} );
	}

	@Test
	public void testChildHasChanged(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			List list = queryForPropertyHasChanged( auditReader, ChildEntity.class, id1, "data" );
			assertEquals( 2, list.size() );
			assertEquals( makeList( 1, 2 ), extractRevisionNumbers( list ) );

			list = queryForPropertyHasChanged( auditReader, ChildEntity.class, id1, "numVal" );
			assertEquals( 2, list.size() );
			assertEquals( makeList( 1, 2 ), extractRevisionNumbers( list ) );

			list = queryForPropertyHasNotChanged( auditReader, ChildEntity.class, id1, "data" );
			assertEquals( 0, list.size() );

			list = queryForPropertyHasNotChanged( auditReader, ChildEntity.class, id1, "numVal" );
			assertEquals( 0, list.size() );
		} );
	}

	@Test
	public void testParentHasChanged(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			List list = queryForPropertyHasChanged( auditReader, ParentEntity.class, id1, "data" );
			assertEquals( 2, list.size() );
			assertEquals( makeList( 1, 2 ), extractRevisionNumbers( list ) );

			list = queryForPropertyHasNotChanged( auditReader, ParentEntity.class, id1, "data" );
			assertEquals( 0, list.size() );
		} );
	}
}
