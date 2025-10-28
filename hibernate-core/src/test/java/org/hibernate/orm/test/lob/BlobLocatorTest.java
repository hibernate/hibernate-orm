/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.lob;

import junit.framework.AssertionFailedError;
import org.hibernate.LockMode;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.Blob;
import java.sql.SQLException;
import java.util.Arrays;

import static org.hibernate.Hibernate.getLobHelper;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests lazy materialization of data mapped by
 * {@link org.hibernate.type.StandardBasicTypes#BLOB}, as well as bounded and unbounded
 * materialization and mutation.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsExpectedLobUsagePattern.class )
@DomainModel(xmlMappings = "org/hibernate/orm/test/lob/LobMappings.hbm.xml")
@SessionFactory
public class BlobLocatorTest {
	private static final long BLOB_SIZE = 10000L;

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testBoundedBlobLocatorAccess(SessionFactoryScope factoryScope) throws Exception {
		byte[] original = buildByteArray( BLOB_SIZE, true );
		byte[] changed = buildByteArray( BLOB_SIZE, false );
		byte[] empty = new byte[] {};

		final var dialect = factoryScope.getSessionFactory().getJdbcServices().getDialect();

		var id = factoryScope.fromTransaction( (s) -> {
			var entity = new LobHolder();
			entity.setBlobLocator( getLobHelper().createBlob( original ) );
			s.persist( entity );
			s.flush();
			return entity.getId();
		} );

		factoryScope.inTransaction( (s) -> {
			try {
				var entity = s.find( LobHolder.class, id );
				assertEquals( BLOB_SIZE, entity.getBlobLocator().length() );
				assertEqualBytes( original, extractData( entity.getBlobLocator() ) );
			}
			catch (SQLException e) {
				throw new RuntimeException( e );
			}
		} );

		// test mutation via setting the new clob data...
		if ( dialect.supportsLobValueChangePropagation() ) {
			factoryScope.inTransaction( (s) -> {
				try {
					var entity = s.find( LobHolder.class, id, LockMode.PESSIMISTIC_WRITE );
					entity.getBlobLocator().truncate( 1 );
					entity.getBlobLocator().setBytes( 1, changed );
				}
				catch (SQLException e) {
					throw new RuntimeException( e );
				}
			} );

			factoryScope.inTransaction( (s) -> {
				try {
					var entity = s.find( LobHolder.class, id, LockMode.PESSIMISTIC_WRITE );
					Assertions.assertNotNull( entity.getBlobLocator() );
					assertEquals( BLOB_SIZE, entity.getBlobLocator().length() );
					assertEqualBytes( changed, extractData( entity.getBlobLocator() ) );
					entity.getBlobLocator().truncate( 1 );
					entity.getBlobLocator().setBytes( 1, original );
				}
				catch (SQLException e) {
					throw new RuntimeException( e );
				}
			} );
		}

		// test mutation via supplying a new clob locator instance...
		factoryScope.inTransaction( (s) -> {
			try {
				var entity = s.find( LobHolder.class, id, LockMode.PESSIMISTIC_WRITE );
				Assertions.assertNotNull( entity.getBlobLocator() );
				assertEquals( BLOB_SIZE, entity.getBlobLocator().length() );
				assertEqualBytes( original, extractData( entity.getBlobLocator() ) );
				entity.setBlobLocator( getLobHelper().createBlob( changed ) );
			}
			catch (SQLException e) {
				throw new RuntimeException( e );
			}
		} );

		// test empty blob
		factoryScope.inTransaction( (s) -> {
			try {
				var entity = s.find( LobHolder.class, id );
				assertEquals( BLOB_SIZE, entity.getBlobLocator().length() );
				assertEqualBytes( changed, extractData( entity.getBlobLocator() ) );
				entity.setBlobLocator( getLobHelper().createBlob( empty ) );
			}
			catch (SQLException e) {
				throw new RuntimeException( e );
			}
		} );

		factoryScope.inTransaction( (s) -> {
			var entity = s.find( LobHolder.class, id );
			// Seems ASE does not support empty BLOBs
			if ( entity.getBlobLocator() != null && !(dialect instanceof SybaseDialect) ) {
				try {
					assertEquals( empty.length, entity.getBlobLocator().length() );
					assertEqualBytes( empty, extractData( entity.getBlobLocator() ) );
				}
				catch (SQLException e) {
					throw new RuntimeException( e );
				}
			}
			s.remove( entity );
		} );
	}

	@Test
	@RequiresDialectFeature(
			feature = DialectFeatureChecks.SupportsUnboundedLobLocatorMaterializationCheck.class,
			comment = "database/driver does not support materializing a LOB locator outside the owning transaction"
	)
	public void testUnboundedBlobLocatorAccess(SessionFactoryScope factoryScope) throws Throwable {
		// Note: unbounded mutation of the underlying lob data is completely
		// unsupported; most databases would not allow such a construct anyway.
		// Thus, here we are only testing materialization...

		byte[] original = buildByteArray( BLOB_SIZE, true );

		var id = factoryScope.fromTransaction( (s) -> {
			var entity = new LobHolder();
			entity.setBlobLocator( getLobHelper().createBlob( original ) );
			s.persist( entity );
			s.flush();
			return entity.getId();
		} );

		// load the entity with the clob locator, and close the session/transaction;
		// at that point it is unbounded...
		var detached = factoryScope.fromTransaction( (s) -> {
			return s.find(  LobHolder.class, id );
		} );

		try {
			assertEquals( BLOB_SIZE, detached.getBlobLocator().length() );
			assertEqualBytes( original, extractData( detached.getBlobLocator() ) );
		}
		catch (SQLException e) {
			throw new RuntimeException( e );
		}
	}

	public static byte[] extractData(Blob blob) throws RuntimeException {
		try {
			return blob.getBytes( 1, ( int ) blob.length() );
		}
		catch (SQLException e) {
			throw new RuntimeException( e );
		}
	}


	public static byte[] buildByteArray(long size, boolean on) {
		byte[] data = new byte[(int)size];
		data[0] = mask( on );
		for ( int i = 0; i < size; i++ ) {
			data[i] = mask( on );
			on = !on;
		}
		return data;
	}

	private static byte mask(boolean on) {
		return on ? ( byte ) 1 : ( byte ) 0;
	}

	public static void assertEqualBytes(byte[] val1, byte[] val2) {
		if ( !Arrays.equals( val1, val2 ) ) {
			throw new AssertionFailedError( "byte arrays did not match" );
		}
	}
}
