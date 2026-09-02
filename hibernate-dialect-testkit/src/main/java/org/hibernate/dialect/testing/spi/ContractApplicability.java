/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.testing.spi;

import java.util.Objects;

import org.hibernate.Incubating;
import org.hibernate.SPI;

import static org.hibernate.SPI.Role.USE;

/// States whether an optional contract applies to a Dialect profile.
///
/// @author Steve Ebersole
/// @since 8.0
@Incubating
@SPI(USE)
public final class ContractApplicability {
	private static final ContractApplicability APPLICABLE = new ContractApplicability( true, "" );
	private final boolean applicable;
	private final String reason;

	private ContractApplicability(boolean applicable, String reason) {
		this.applicable = applicable;
		this.reason = reason == null ? "" : reason.strip();
		if ( !applicable && this.reason.isEmpty() ) {
			throw new IllegalArgumentException( "An inapplicable contract requires a nonblank reason" );
		}
	}

	/// Whether the contract applies to the profile.
	public boolean isApplicable() {
		return applicable;
	}

	/// The reason an optional contract is inapplicable, or an empty string.
	public String reason() {
		return reason;
	}

	/// Mark a contract applicable.
	public static ContractApplicability applicable() {
		return APPLICABLE;
	}

	/// Mark an optional contract inapplicable and explain why.
	public static ContractApplicability inapplicable(String reason) {
		return new ContractApplicability( false, reason );
	}

	@Override
	public boolean equals(Object object) {
		return this == object
				|| object instanceof ContractApplicability other
				&& applicable == other.applicable
				&& reason.equals( other.reason );
	}

	@Override
	public int hashCode() {
		return Objects.hash( applicable, reason );
	}

	@Override
	public String toString() {
		return applicable ? "applicable" : "inapplicable: " + reason;
	}
}
