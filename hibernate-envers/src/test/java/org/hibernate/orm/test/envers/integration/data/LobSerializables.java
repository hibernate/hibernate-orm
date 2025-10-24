/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.data;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsExpectedLobUsagePattern.class)
@EnversTest
@Jpa(annotatedClasses = {LobSerializableTestEntity.class},
		integrationSettings = @Setting(name = "hibernate.connection.autocommit", value = "false"))
public class LobSerializables {
	private Integer id1;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		id1 = scope.fromTransaction( em -> {
			LobSerializableTestEntity ste = new LobSerializableTestEntity( new SerObject( "d1" ) );
			em.persist( ste );
			return ste.getId();
		} );

		scope.inTransaction( em -> {
			LobSerializableTestEntity ste = em.find( LobSerializableTestEntity.class, id1 );
			ste.setObj( new SerObject( "d2" ) );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( LobSerializableTestEntity.class, id1 ) );
		} );
	}

	@Test
	public void testHistoryOfId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			LobSerializableTestEntity ver1 = new LobSerializableTestEntity( id1, new SerObject( "d1" ) );
			LobSerializableTestEntity ver2 = new LobSerializableTestEntity( id1, new SerObject( "d2" ) );

			assertEquals( ver1, auditReader.find( LobSerializableTestEntity.class, id1, 1 ) );
			assertEquals( ver2, auditReader.find( LobSerializableTestEntity.class, id1, 2 ) );
		} );
	}
}
