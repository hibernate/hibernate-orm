/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.internal;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.dialect.lock.spi.BlockingDuration;
import org.hibernate.dialect.lock.spi.TransactionConcurrencyResolutionException;
import org.hibernate.engine.jdbc.env.JdbcMetadataOnBoot;
import org.jboss.logging.Logger;

/// HyperSQL ordinary reads depend on its LOCKS, MVLOCKS, or MVCC transaction
/// control model. FOR UPDATE controls cursor updatability, not lock acquisition,
/// and therefore does not establish an explicit current-read or row-lock strategy.
///
/// @since 8.0
/// @author Steve Ebersole
public final class HSQLTransactionConcurrencyResolver extends AbstractTransactionConcurrencyResolver {
	public static final HSQLTransactionConcurrencyResolver INSTANCE = new HSQLTransactionConcurrencyResolver();

	private HSQLTransactionConcurrencyResolver() {
	}

	@Override
	protected Boolean observeVersionedReads(Integer isolation, Connection connection, JdbcMetadataOnBoot access) {
		if ( connection == null || access == JdbcMetadataOnBoot.DISALLOW ) {
			return null;
		}
		try ( var statement = connection.createStatement();
				var rows = statement.executeQuery(
						"select property_value from information_schema.system_properties where property_name = 'hsqldb.tx'" ) ) {
			if ( !rows.next() || rows.getString( 1 ) == null ) {
				throw new SQLException( "No HyperSQL transaction control model returned" );
			}
			return switch ( rows.getString( 1 ).toUpperCase( java.util.Locale.ROOT ) ) {
				case "LOCKS" -> false;
				case "MVCC" -> true;
				// MVLOCKS uses snapshots for read-only transactions, locking otherwise.
				case "MVLOCKS" -> connection.isReadOnly();
				default -> throw new SQLException( "Unknown HyperSQL transaction control model" );
			};
		}
		catch (SQLException ex) {
			if ( access == JdbcMetadataOnBoot.REQUIRE ) {
				throw new TransactionConcurrencyResolutionException( "Unable to resolve HyperSQL transaction control", ex );
			}
			Logger.getLogger( HSQLTransactionConcurrencyResolver.class )
					.warn( "Unable to observe HyperSQL transaction control", ex );
			return null;
		}
	}

	@Override
	protected boolean dirtyReads(Integer isolation) {
		return false;
	}

	@Override
	protected BlockingDuration writeWriteBlocking(Integer isolation) {
		return isolation != null && isolation == Connection.TRANSACTION_NONE
				? BlockingDuration.UNKNOWN : BlockingDuration.TRANSACTION;
	}
}
