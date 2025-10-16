/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.modifiedflags;

import java.util.List;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.orm.test.envers.integration.basic.BasicTestEntity1;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.orm.test.envers.integration.modifiedflags.AbstractModifiedFlagsEntityTest.queryForPropertyHasChanged;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-11582")
@EnversTest
@Jpa(
		integrationSettings = @Setting(name = EnversSettings.GLOBAL_WITH_MODIFIED_FLAG, value = "true"),
		annotatedClasses = {BasicTestEntity1.class}
)
public class HasChangedInsertUpdateSameTransactionTest extends AbstractModifiedFlagsEntityTest {

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			// Revision 1
			em.getTransaction().begin();
			BasicTestEntity1 entity = new BasicTestEntity1( "str1", 1 );
			em.persist( entity );
			entity.setStr1( "str2" );
			em.merge( entity );
			em.getTransaction().commit();
		} );
	}

	@Test
	public void testPropertyChangedInsrtUpdateSameTransaction(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			// this was only flagged as changed as part of the persist
			List list = queryForPropertyHasChanged( AuditReaderFactory.get( em ), BasicTestEntity1.class, 1, "long1" );
			assertEquals( 1, list.size() );
		} );
	}
}
