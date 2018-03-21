/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

// $Id$
package org.hibernate.test.annotations.namingstrategy.charset;

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
		return "UKq2jxex2hrvg4139p85npyj71g";
	}

	@Override
	protected String expectedForeignKeyName() {
		return "FKdeqq4y6cesc2yfgi97u2hp61g";
	}

	@Override
	protected String expectedIndexName() {
		return "IDXq2jxex2hrvg4139p85npyj71g";
	}
}
