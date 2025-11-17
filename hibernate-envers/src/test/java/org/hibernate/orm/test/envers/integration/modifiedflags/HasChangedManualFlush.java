/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.modifiedflags;

import java.util.List;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.orm.test.envers.integration.basic.BasicTestEntity1;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.orm.test.envers.integration.modifiedflags.AbstractModifiedFlagsEntityTest.queryForPropertyHasChanged;
import static org.hibernate.orm.test.envers.tools.TestTools.extractRevisionNumbers;
import static org.hibernate.orm.test.envers.tools.TestTools.makeList;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-7918")
@Jpa(
		integrationSettings = @Setting(name = EnversSettings.GLOBAL_WITH_MODIFIED_FLAG, value = "true"),
		annotatedClasses = {BasicTestEntity1.class}
)
public class HasChangedManualFlush extends AbstractModifiedFlagsEntityTest {
	private Integer id = null;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			// Revision 1
			em.getTransaction().begin();
			BasicTestEntity1 entity = new BasicTestEntity1( "str1", 1 );
			em.persist( entity );
			em.getTransaction().commit();

			id = entity.getId();

			// Revision 2 - both properties (str1 and long1) should be marked as modified.
			em.getTransaction().begin();
			BasicTestEntity1 entity2 = em.find( BasicTestEntity1.class, entity.getId() );
			entity2.setStr1( "str2" );
			entity2 = em.merge( entity2 );
			em.flush();
			entity2.setLong1( 2 );
			entity2 = em.merge( entity2 );
			em.flush();
			em.getTransaction().commit();
		} );
	}

	@Test
	public void testHasChangedOnDoubleFlush(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			List list = queryForPropertyHasChanged( auditReader, BasicTestEntity1.class, id, "str1" );
			assertEquals( 2, list.size() );
			assertEquals( makeList( 1, 2 ), extractRevisionNumbers( list ) );

			list = queryForPropertyHasChanged( auditReader, BasicTestEntity1.class, id, "long1" );
			assertEquals( 2, list.size() );
			assertEquals( makeList( 1, 2 ), extractRevisionNumbers( list ) );
		} );
	}
}
