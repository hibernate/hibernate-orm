/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

// $Id$
package org.hibernate.test.annotations.namingstrategy.charset;

import org.hibernate.dialect.AbstractHANADialect;
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
		if ( this.serviceRegistry.getService( JdbcServices.class ).getDialect() instanceof AbstractHANADialect ) {
			return "UK38xspy14r49kkcmmyltias1j4"; // Non-ASCII, non-alphanumeric identifiers are quoted on HANA
		}
		else {
			return "UKq2jxex2hrvg4139p85npyj71g";
		}
	}

	@Override
	protected String expectedForeignKeyName() {
		if ( this.serviceRegistry.getService( JdbcServices.class ).getDialect() instanceof AbstractHANADialect ) {
			return "FKdvmx00nr88d03v6xhrjyujrq2"; // Non-ASCII, non-alphanumeric identifiers are quoted on HANA
		}
		else {
			return "FKdeqq4y6cesc2yfgi97u2hp61g";
		}
	}

	@Override
	protected String expectedIndexName() {
		if ( this.serviceRegistry.getService( JdbcServices.class ).getDialect() instanceof AbstractHANADialect ) {
			return "IDX38xspy14r49kkcmmyltias1j4"; // Non-ASCII, non-alphanumeric identifiers are quoted on HANA
		}
		else {
			return "IDXq2jxex2hrvg4139p85npyj71g";
		}
	}
}
