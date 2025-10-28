/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.lob;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.hibernate.Hibernate.getLobHelper;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey( value = "HHH-2680" )
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsExpectedLobUsagePattern.class )
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsLobValueChangePropagation.class )
@DomainModel(xmlMappings = "org/hibernate/orm/test/lob/LobMappings.hbm.xml")
@SessionFactory
public class LobMergeTest {
	private static final int LOB_SIZE = 10000;

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testMergingBlobData(SessionFactoryScope factoryScope) throws Exception {
		final byte[] original = BlobLocatorTest.buildByteArray( LOB_SIZE, true );
		final byte[] updated = BlobLocatorTest.buildByteArray( LOB_SIZE, false );

		var detached = factoryScope.fromTransaction( (s) -> {
			LobHolder entity = new LobHolder();
			entity.setBlobLocator( getLobHelper().createBlob( original ) );
			s.persist( entity );
			return entity;
		} );

		factoryScope.inTransaction( (s) -> {
			// entity still detached...
			detached.setBlobLocator( getLobHelper().createBlob( updated ) );
			s.merge( detached );
		} );

		factoryScope.inTransaction( (s) -> {
			try {
				var entity = s.find( LobHolder.class, detached.getId() );
				Assertions.assertEquals( LOB_SIZE, entity.getBlobLocator().length(),
						"blob sizes did not match after merge" );
				Assertions.assertArrayEquals( updated, BlobLocatorTest.extractData( entity.getBlobLocator() ),
						"blob contents did not match after merge" );
				s.remove( entity );
			}
			catch (SQLException e) {
				throw new RuntimeException( e );
			}
		} );
	}

	@Test
	public void testMergingClobData(SessionFactoryScope factoryScope) throws Exception {
		final String original = ClobLocatorTest.buildString( LOB_SIZE, 'a' );
		final String updated = ClobLocatorTest.buildString( LOB_SIZE, 'z' );

		var detached = factoryScope.fromTransaction( (s) -> {
			LobHolder entity = new LobHolder();
			entity.setClobLocator( getLobHelper().createClob( original ) );
			s.persist( entity );
			return entity;
		} );

		var detached2 = factoryScope.fromTransaction( (s) -> {
			// entity still detached...
			detached.setClobLocator( getLobHelper().createClob( updated ) );
			return s.merge( detached );
		} );

		factoryScope.inTransaction( (s) -> {
			try {
				var entity = s.find( LobHolder.class, detached.getId() );
				Assertions.assertEquals( LOB_SIZE, entity.getClobLocator().length(),
						"clob sizes did not match after merge" );
				Assertions.assertEquals( updated, ClobLocatorTest.extractData( entity.getClobLocator() ),
						"clob contents did not match after merge" );
				s.remove( entity );
			}
			catch (Exception e) {
				throw new RuntimeException( e );
			}
		} );
	}
}
