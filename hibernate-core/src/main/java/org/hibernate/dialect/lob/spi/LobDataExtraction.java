/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lob.spi;

import java.io.InputStream;
import java.sql.Clob;

import org.hibernate.Incubating;
import org.hibernate.SPI;
import org.hibernate.type.descriptor.java.DataHelper;

import static org.hibernate.SPI.Role.USE;

/// Materializes LOB data using Hibernate's standard resource-handling and
/// exception-conversion semantics.
///
/// Use these operations when a Dialect-specific JDBC type must materialize a
/// driver LOB. The stream or reader used for extraction is closed; the caller
/// remains responsible for freeing the JDBC LOB locator.
///
/// @author Steve Ebersole
/// @since 8.0
@Incubating
@SPI(USE)
public final class LobDataExtraction {
	private LobDataExtraction() {
	}

	/// Read all bytes from the supplied stream and close it.
	public static byte[] extractBytes(InputStream inputStream) {
		return DataHelper.extractBytes( inputStream );
	}

	/// Read all characters from the supplied CLOB and close its character
	/// stream without freeing the CLOB locator.
	public static String extractString(Clob clob) {
		return DataHelper.extractString( clob );
	}
}
