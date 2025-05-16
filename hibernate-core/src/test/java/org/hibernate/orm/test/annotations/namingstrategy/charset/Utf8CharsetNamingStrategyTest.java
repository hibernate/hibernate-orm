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
			return "UK3punuckhqfc03ddypdrpeahs9cty";
		}
	}

	@Override
	protected String expectedForeignKeyName() {
		if ( this.serviceRegistry.getService( JdbcServices.class ).getDialect() instanceof HANADialect ) {
			return "FKe1lr9dd16cmmon53r7m736yev"; // Non-ASCII, non-alphanumeric identifiers are quoted on HANA
		}
		else {
			return "FK6av084md8iluhsdmw8hqv4ep8g98";
		}
	}

	@Override
	protected String expectedIndexName() {
		if ( this.serviceRegistry.getService( JdbcServices.class ).getDialect() instanceof HANADialect ) {
			return "IDXinnacp0woeltj5l0k4vgabf8k"; // Non-ASCII, non-alphanumeric identifiers are quoted on HANA
		}
		else {
			return "IDX3punuckhqfc03ddypdrpeahs9ct";
		}
	}
}
