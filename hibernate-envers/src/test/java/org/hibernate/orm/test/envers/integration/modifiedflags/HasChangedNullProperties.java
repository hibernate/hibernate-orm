/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.modifiedflags;

import java.util.List;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.orm.test.envers.integration.basic.BasicTestEntity1;
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
		annotatedClasses = {BasicTestEntity1.class})
public class HasChangedNullProperties extends AbstractModifiedFlagsEntityTest {
	private Integer id1;
	private Integer id2;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			em.getTransaction().begin();
			BasicTestEntity1 bte1 = new BasicTestEntity1( "x", 1 );
			em.persist( bte1 );
			em.getTransaction().commit();
			id1 = bte1.getId();
		} );

		scope.inEntityManager( em -> {
			em.getTransaction().begin();
			BasicTestEntity1 bte2 = new BasicTestEntity1( null, 20 );
			em.persist( bte2 );
			em.getTransaction().commit();
			id2 = bte2.getId();
		} );

		scope.inEntityManager( em -> {
			em.getTransaction().begin();
			BasicTestEntity1 bte1 = em.find( BasicTestEntity1.class, id1 );
			bte1.setLong1( 1 );
			bte1.setStr1( null );
			em.getTransaction().commit();
		} );

		scope.inEntityManager( em -> {
			em.getTransaction().begin();
			BasicTestEntity1 bte2 = em.find( BasicTestEntity1.class, id2 );
			bte2.setLong1( 20 );
			bte2.setStr1( "y2" );
			em.getTransaction().commit();
		} );
	}

	@Test
	public void testHasChanged(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			List list = AbstractModifiedFlagsEntityTest.queryForPropertyHasChangedWithDeleted(
					auditReader,
					BasicTestEntity1.class,
					id1,
					"str1"
			);
			assertEquals( 2, list.size() );
			assertEquals( makeList( 1, 3 ), extractRevisionNumbers( list ) );

			list = AbstractModifiedFlagsEntityTest.queryForPropertyHasChangedWithDeleted(
					auditReader,
					BasicTestEntity1.class,
					id1,
					"long1"
			);
			assertEquals( 1, list.size() );
			assertEquals( makeList( 1 ), extractRevisionNumbers( list ) );

			list = AbstractModifiedFlagsEntityTest.queryForPropertyHasChangedWithDeleted(
					auditReader,
					BasicTestEntity1.class,
					id2,
					"str1"
			);
			// str1 property was null before insert and after insert so in a way it didn't change - is it a good way to go?
			assertEquals( 1, list.size() );
			assertEquals( makeList( 4 ), extractRevisionNumbers( list ) );

			list = AbstractModifiedFlagsEntityTest.queryForPropertyHasChangedWithDeleted(
					auditReader,
					BasicTestEntity1.class,
					id2,
					"long1"
			);
			assertEquals( 1, list.size() );
			assertEquals( makeList( 2 ), extractRevisionNumbers( list ) );

			list = auditReader.createQuery().forRevisionsOfEntity( BasicTestEntity1.class, false, true )
					.add( AuditEntity.property( "str1" ).hasChanged() )
					.add( AuditEntity.property( "long1" ).hasChanged() )
					.getResultList();
			assertEquals( 1, list.size() );
			assertEquals( makeList( 1 ), extractRevisionNumbers( list ) );
		} );
	}
}
