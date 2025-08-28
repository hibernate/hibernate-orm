/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.namingstrategy.charset;

import org.hibernate.dialect.HANADialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;

/**
 * @author Vlad Mihalcea
 */
public class Utf8CharsetNamingStrategyTest extends AbstractCharsetNamingStrategyTest {

	@Override
	protected String charsetName() {
		return "UTF8";
	}

	@Override
	protected String expectedUniqueKeyName() {
		if ( this.serviceRegistry.getService( JdbcServices.class ).getDialect() instanceof HANADialect ) {
			return "UKinnacp0woeltj5l0k4vgabf8k"; // Non-ASCII, non-alphanumeric identifiers are quoted on HANA
		}
		else {
			return "UKpm66tdjkgtsca5x2uwux487t5";
		}
	}

	@Override
	protected String expectedForeignKeyName() {
		if ( this.serviceRegistry.getService( JdbcServices.class ).getDialect() instanceof HANADialect ) {
			return "FKe1lr9dd16cmmon53r7m736yev"; // Non-ASCII, non-alphanumeric identifiers are quoted on HANA
		}
		else {
			return "FKgvrnki5fwp3qo0hfp1bu1jj0q";
		}
	}

	@Override
	protected String expectedIndexName() {
		if ( this.serviceRegistry.getService( JdbcServices.class ).getDialect() instanceof HANADialect ) {
			return "IDXinnacp0woeltj5l0k4vgabf8k"; // Non-ASCII, non-alphanumeric identifiers are quoted on HANA
		}
		else {
			return "IDXpm66tdjkgtsca5x2uwux487t5";
		}
	}
}
