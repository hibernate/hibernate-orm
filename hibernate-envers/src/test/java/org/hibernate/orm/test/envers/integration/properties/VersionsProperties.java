/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.properties;

import java.util.Arrays;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@Jpa(annotatedClasses = {PropertiesTestEntity.class},
		integrationSettings = {
				@Setting(name = EnversSettings.AUDIT_TABLE_PREFIX, value = "VP_"),
				@Setting(name = EnversSettings.AUDIT_TABLE_SUFFIX, value = "_VS"),
				@Setting(name = EnversSettings.REVISION_FIELD_NAME, value = "ver_rev"),
				@Setting(name = EnversSettings.REVISION_TYPE_FIELD_NAME, value = "ver_rev_type")
		})
public class VersionsProperties {
	private Integer id1;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			PropertiesTestEntity pte = new PropertiesTestEntity( "x" );
			em.persist( pte );
			id1 = pte.getId();
		} );

		scope.inTransaction( em -> {
			PropertiesTestEntity pte = em.find( PropertiesTestEntity.class, id1 );
			pte.setStr( "y" );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( PropertiesTestEntity.class, id1 ) );
		} );
	}

	@Test
	public void testHistoryOfId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			PropertiesTestEntity ver1 = new PropertiesTestEntity( id1, "x" );
			PropertiesTestEntity ver2 = new PropertiesTestEntity( id1, "y" );

			assertEquals( ver1, auditReader.find( PropertiesTestEntity.class, id1, 1 ) );
			assertEquals( ver2, auditReader.find( PropertiesTestEntity.class, id1, 2 ) );
		} );
	}
}
