/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.modifiedflags;

import java.util.List;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.orm.test.envers.integration.basic.BasicTestEntity2;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import static org.hibernate.orm.test.envers.tools.TestTools.extractRevisionNumbers;
import static org.hibernate.orm.test.envers.tools.TestTools.makeList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
@Jpa(integrationSettings = @Setting(name = EnversSettings.GLOBAL_WITH_MODIFIED_FLAG, value = "true"),
		annotatedClasses = {BasicTestEntity2.class})
public class HasChangedUnversionedProperties extends AbstractModifiedFlagsEntityTest {
	private Integer id1;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			em.getTransaction().begin();
			BasicTestEntity2 bte2 = new BasicTestEntity2( "x", "a" );
			em.persist( bte2 );
			em.getTransaction().commit();
			id1 = bte2.getId();
		} );

		scope.inEntityManager( em -> {
			em.getTransaction().begin();
			BasicTestEntity2 bte2 = em.find( BasicTestEntity2.class, id1 );
			bte2.setStr1( "x" );
			bte2.setStr2( "a" );
			em.getTransaction().commit();
		} );

		scope.inEntityManager( em -> {
			em.getTransaction().begin();
			BasicTestEntity2 bte2 = em.find( BasicTestEntity2.class, id1 );
			bte2.setStr1( "y" );
			bte2.setStr2( "b" );
			em.getTransaction().commit();
		} );

		scope.inEntityManager( em -> {
			em.getTransaction().begin();
			BasicTestEntity2 bte2 = em.find( BasicTestEntity2.class, id1 );
			bte2.setStr1( "y" );
			bte2.setStr2( "c" );
			em.getTransaction().commit();
		} );
	}

	@Test
	public void testHasChangedQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			List list = AbstractModifiedFlagsEntityTest.queryForPropertyHasChanged(
					auditReader,
					BasicTestEntity2.class,
					id1,
					"str1"
			);
			assertEquals( 2, list.size() );
			assertEquals( makeList( 1, 2 ), extractRevisionNumbers( list ) );
		} );
	}

	@Test
	public void testExceptionOnHasChangedQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertThrows( IllegalArgumentException.class, () ->
					AbstractModifiedFlagsEntityTest.queryForPropertyHasChangedWithDeleted(
							auditReader,
							BasicTestEntity2.class,
							id1,
							"str2"
					)
			);
		} );
	}
}
