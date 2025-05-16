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
public class Iso88591CharsetNamingStrategyTest extends AbstractCharsetNamingStrategyTest {

	@Override
	protected String charsetName() {
		return "ISO-8859-1";
	}

	@Override
	protected String expectedUniqueKeyName() {
		if ( this.serviceRegistry.getService( JdbcServices.class ).getDialect() instanceof HANADialect ) {
			return "UK38xspy14r49kkcmmyltias1j4"; // Non-ASCII, non-alphanumeric identifiers are quoted on HANA
		}
		else {
			return "UK8k5luacfo75uusg9a3fb87wbeub3";
		}
	}

	@Override
	protected String expectedForeignKeyName() {
		if ( this.serviceRegistry.getService( JdbcServices.class ).getDialect() instanceof HANADialect ) {
			return "FKdvmx00nr88d03v6xhrjyujrq2"; // Non-ASCII, non-alphanumeric identifiers are quoted on HANA
		}
		else {
			return "FKgkcc85inbuuppywsp0bxqfbqknno";
		}
	}

	@Override
	protected String expectedIndexName() {
		if ( this.serviceRegistry.getService( JdbcServices.class ).getDialect() instanceof HANADialect ) {
			return "IDX38xspy14r49kkcmmyltias1j4"; // Non-ASCII, non-alphanumeric identifiers are quoted on HANA
		}
		else {
			return "IDX8k5luacfo75uusg9a3fb87wbeub";
		}
	}
}
