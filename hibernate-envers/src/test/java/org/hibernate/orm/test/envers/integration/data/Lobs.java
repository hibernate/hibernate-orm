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
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsExpectedLobUsagePattern.class)
@EnversTest
@Jpa(annotatedClasses = {LobTestEntity.class},
		integrationSettings = @Setting(name = "hibernate.connection.autocommit", value = "false"))
public class Lobs {
	private Integer id1;
	private Integer id2;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		id1 = scope.fromTransaction( em -> {
			LobTestEntity lte = new LobTestEntity( "abc", new byte[] {0, 1, 2}, new char[] {'x', 'y', 'z'} );
			em.persist( lte );
			return lte.getId();
		} );

		scope.inTransaction( em -> {
			LobTestEntity lte = em.find( LobTestEntity.class, id1 );
			lte.setStringLob( "def" );
			lte.setByteLob( new byte[] {3, 4, 5} );
			lte.setCharLob( new char[] {'h', 'i', 'j'} );
		} );

		// this creates a revision history for a Lob-capable entity but the change is on a non-audited
		// field and so it should only generate 1 revision, the initial persist.
		id2 = scope.fromTransaction( em -> {
			LobTestEntity lte2 = new LobTestEntity( "abc", new byte[] {0, 1, 2}, new char[] {'x', 'y', 'z'} );
			lte2.setData( "Hi" );
			em.persist( lte2 );
			return lte2.getId();
		} );

		scope.inTransaction( em -> {
			LobTestEntity lte2 = em.find( LobTestEntity.class, id2 );
			lte2.setData( "Hello World" );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( LobTestEntity.class, id1 ) );
		} );
	}

	@Test
	public void testHistoryOfId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			LobTestEntity ver1 = new LobTestEntity( id1, "abc", new byte[] {0, 1, 2}, new char[] {'x', 'y', 'z'} );
			LobTestEntity ver2 = new LobTestEntity( id1, "def", new byte[] {3, 4, 5}, new char[] {'h', 'i', 'j'} );
			assertEquals( ver1, auditReader.find( LobTestEntity.class, id1, 1 ) );
			assertEquals( ver2, auditReader.find( LobTestEntity.class, id1, 2 ) );
		} );
	}

	@Test
	@JiraKey(value = "HHH-10734")
	public void testRevisionsCountsForAuditedArraysWithNoChanges(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 3 ), auditReader.getRevisions( LobTestEntity.class, id2 ) );
		} );
	}

	@Test
	@JiraKey(value = "HHH-10734")
	public void testHistoryOfId2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			LobTestEntity ver1 = new LobTestEntity( id2, "abc", new byte[] {0, 1, 2}, new char[] {'x', 'y', 'z'} );
			assertEquals( ver1, auditReader.find( LobTestEntity.class, id2, 3 ) );
		} );
	}
}
