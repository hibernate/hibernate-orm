/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.lob;

import org.hibernate.LockMode;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.descriptor.java.DataHelper;
import org.junit.jupiter.api.Test;

import java.sql.Clob;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests lazy materialization of data mapped by
 * {@link org.hibernate.type.StandardBasicTypes#CLOB} as well as bounded and unbounded
 * materialization and mutation.
 *
 * @author Steve Ebersole
 */
@RequiresDialectFeature(
		feature = DialectFeatureChecks.SupportsExpectedLobUsagePattern.class,
		comment = "database/driver does not support expected LOB usage pattern"
)
@RequiresDialectFeature(
		feature = DialectFeatureChecks.SupportsUnboundedLobLocatorMaterializationCheck.class,
		comment = "database/driver does not support expected LOB usage pattern"
)
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/lob/LobMappings.hbm.xml"
)
@SessionFactory
public class ClobLocatorTest {
	private static final int CLOB_SIZE = 10000;


	public String[] getMappings() {
		return new String[] { "" };
	}

	@Test
	public void testBoundedClobLocatorAccess(SessionFactoryScope scope) throws Throwable {
		String original = buildString( CLOB_SIZE, 'x' );
		String changed = buildString( CLOB_SIZE, 'y' );
		String empty = "";

		Long id = scope.fromTransaction(
				session -> {
					LobHolder entity = new LobHolder();
					entity.setClobLocator( session.getLobHelper().createClob( original ) );
					session.persist( entity );
					return entity.getId();
				}
		);

		scope.inTransaction(
				session -> {
					try {
						LobHolder entity = session.get( LobHolder.class, id );
						assertEquals( CLOB_SIZE, entity.getClobLocator().length() );
						assertEquals( original, extractData( entity.getClobLocator() ) );
					}
					catch (Exception e) {
						fail( e );
					}
				}
		);

		// test mutation via setting the new clob data...
		if ( scope.getSessionFactory().getJdbcServices().getDialect().supportsLobValueChangePropagation() ) {
			scope.inTransaction(
					session -> {
						try {
							LobHolder entity = session.byId( LobHolder.class )
									.with( LockMode.PESSIMISTIC_WRITE )
									.load( id );
							entity.getClobLocator().truncate( 1 );
							entity.getClobLocator().setString( 1, changed );
						}
						catch (Exception e) {
							fail( e );
						}
					}
			);

			scope.inTransaction(
					session -> {
						try {
							LobHolder entity = session.byId( LobHolder.class )
									.with( LockMode.PESSIMISTIC_WRITE )
									.load( id );
							assertNotNull( entity.getClobLocator() );

							assertEquals( CLOB_SIZE, entity.getClobLocator().length() );

							assertEquals( changed, extractData( entity.getClobLocator() ) );
							entity.getClobLocator().truncate( 1 );
							entity.getClobLocator().setString( 1, original );
						}
						catch (Exception e) {
							fail( e );
						}
					}
			);
		}

		// test mutation via supplying a new clob locator instance...
		scope.inTransaction(
				session -> {
					try {
						LobHolder entity = session.find( LobHolder.class, id, LockMode.PESSIMISTIC_WRITE );
						assertNotNull( entity.getClobLocator() );
						assertEquals( CLOB_SIZE, entity.getClobLocator().length() );
						assertEquals( original, extractData( entity.getClobLocator() ) );
						entity.setClobLocator( session.getLobHelper().createClob( changed ) );
					}
					catch (Exception e) {
						fail( e );
					}
				}
		);

		// test empty clob
		if ( !( scope.getSessionFactory()
				.getJdbcServices()
				.getDialect() instanceof SybaseASEDialect ) ) { // Skip for Sybase. HHH-6425
			scope.inTransaction(
					session -> {
						try {
							LobHolder entity = session.get( LobHolder.class, id );
							assertEquals( CLOB_SIZE, entity.getClobLocator().length() );
							assertEquals( changed, extractData( entity.getClobLocator() ) );
							entity.setClobLocator( session.getLobHelper().createClob( empty ) );
						}
						catch (Exception e) {
							fail( e );
						}
					}
			);

			scope.inTransaction(
					session -> {
						try {
							LobHolder entity = session.get( LobHolder.class, id );
							if ( entity.getClobLocator() != null ) {
								assertEquals( empty.length(), entity.getClobLocator().length() );
								assertEquals( empty, extractData( entity.getClobLocator() ) );
							}
							session.remove( entity );
						}
						catch (Exception e) {
							fail( e );
						}
					}
			);
		}

	}

	@Test
	public void testUnboundedClobLocatorAccess(SessionFactoryScope scope) throws Throwable {
		// Note: unbounded mutation of the underlying lob data is completely
		// unsupported; most databases would not allow such a construct anyway.
		// Thus here we are only testing materialization...

		String original = buildString( CLOB_SIZE, 'x' );

		Long id = scope.fromTransaction(
				session -> {
					LobHolder entity = new LobHolder();
					entity.setClobLocator( session.getLobHelper().createClob( original ) );
					session.persist( entity );
					return entity.getId();
				}
		);

		// load the entity with the clob locator, and close the session/transaction;
		// at that point it is unbounded...
		LobHolder lobHolder = scope.fromTransaction(
				session ->
						session.get( LobHolder.class, id )

		);

		assertEquals( CLOB_SIZE, lobHolder.getClobLocator().length() );
		assertEquals( original, extractData( lobHolder.getClobLocator() ) );

		scope.inTransaction(
				session ->
						session.remove( lobHolder )
		);
	}

	public static String extractData(Clob clob) throws Exception {
		return DataHelper.extractString( clob.getCharacterStream() );
	}

	public static String buildString(int size, char baseChar) {
		StringBuilder buff = new StringBuilder();
		for ( int i = 0; i < size; i++ ) {
			buff.append( baseChar );
		}
		return buff.toString();
	}
}
