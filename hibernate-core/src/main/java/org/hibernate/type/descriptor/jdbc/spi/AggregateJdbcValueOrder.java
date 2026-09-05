/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc.spi;

import java.util.Arrays;

import org.hibernate.Incubating;
import org.hibernate.SPI;

import static java.util.Objects.requireNonNull;
import static org.hibernate.SPI.Role.USE;

/// Describes the physical ordering of the component JDBC values of an
/// aggregate relative to the logical ordering defined by its mapping.
///
/// Create a nonidentity order with [#physicalOrder(int...)]. For each physical
/// position, the supplied value is the logical JDBC-value index stored at that
/// position. For example, `physicalOrder(2, 0, 1)` transforms logical values
/// `[A, B, C]` to physical values `[C, A, B]`.
///
/// Instances are immutable and validate that the supplied indexes form a
/// complete permutation.
///
/// @since 8.0
///
/// @author Steve Ebersole
/// @author Christian Beikov
@Incubating
@SPI(USE)
public final class AggregateJdbcValueOrder {
	private static final AggregateJdbcValueOrder IDENTITY =
			new AggregateJdbcValueOrder( null, null );

	private final int[] logicalIndexByPhysicalPosition;
	private final int[] physicalIndexByLogicalPosition;

	private AggregateJdbcValueOrder(
			int[] logicalIndexByPhysicalPosition,
			int[] physicalIndexByLogicalPosition) {
		this.logicalIndexByPhysicalPosition = logicalIndexByPhysicalPosition;
		this.physicalIndexByLogicalPosition = physicalIndexByLogicalPosition;
	}

	/// Return the order which leaves the logical JDBC-value ordering unchanged.
	///
	/// @return the shared identity order
	public static AggregateJdbcValueOrder identity() {
		return IDENTITY;
	}

	/// Create an order describing which logical JDBC value occurs at each
	/// physical position.
	///
	/// @param logicalIndexByPhysicalPosition the logical index stored at each
	/// physical position
	/// @return an immutable validated order
	///
	/// @throws IllegalArgumentException if the indexes do not form a complete
	/// permutation
	public static AggregateJdbcValueOrder physicalOrder(int... logicalIndexByPhysicalPosition) {
		requireNonNull( logicalIndexByPhysicalPosition, "logicalIndexByPhysicalPosition" );
		if ( logicalIndexByPhysicalPosition.length == 0 ) {
			return IDENTITY;
		}

		final int[] physicalOrder = logicalIndexByPhysicalPosition.clone();
		final int[] logicalOrder = new int[physicalOrder.length];
		final boolean[] encountered = new boolean[physicalOrder.length];
		for ( int physicalIndex = 0; physicalIndex < physicalOrder.length; physicalIndex++ ) {
			final int logicalIndex = physicalOrder[physicalIndex];
			if ( logicalIndex < 0 || logicalIndex >= physicalOrder.length ) {
				throw new IllegalArgumentException(
						"Logical JDBC-value index " + logicalIndex
								+ " at physical position " + physicalIndex
								+ " is outside the valid range [0, " + physicalOrder.length + ")"
				);
			}
			if ( encountered[logicalIndex] ) {
				throw new IllegalArgumentException(
						"Logical JDBC-value index " + logicalIndex + " occurs more than once"
				);
			}
			encountered[logicalIndex] = true;
			logicalOrder[logicalIndex] = physicalIndex;
		}
		return isIdentity( physicalOrder )
				? IDENTITY
				: new AggregateJdbcValueOrder( physicalOrder, logicalOrder );
	}

	private static boolean isIdentity(int[] physicalOrder) {
		for ( int i = 0; i < physicalOrder.length; i++ ) {
			if ( physicalOrder[i] != i ) {
				return false;
			}
		}
		return true;
	}

	int[] physicalOrder(int expectedSize) {
		validateSize( expectedSize );
		return logicalIndexByPhysicalPosition;
	}

	int[] logicalOrder(int expectedSize) {
		validateSize( expectedSize );
		return physicalIndexByLogicalPosition;
	}

	private void validateSize(int expectedSize) {
		if ( logicalIndexByPhysicalPosition != null
				&& logicalIndexByPhysicalPosition.length != expectedSize ) {
			throw new IllegalArgumentException(
					"Aggregate JDBC-value order has " + logicalIndexByPhysicalPosition.length
							+ " positions, but the aggregate mapping has " + expectedSize
			);
		}
	}

	@Override
	public String toString() {
		return logicalIndexByPhysicalPosition == null
				? "AggregateJdbcValueOrder(identity)"
				: "AggregateJdbcValueOrder" + Arrays.toString( logicalIndexByPhysicalPosition );
	}
}
