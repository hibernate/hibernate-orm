/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

// $Id$
package org.hibernate.test.annotations.namingstrategy.charset;

import org.hibernate.testing.TestForIssue;

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
		return "UKpm66tdjkgtsca5x2uwux487t5";
	}

	@Override
	protected String expectedForeignKeyName() {
		return "FKgvrnki5fwp3qo0hfp1bu1jj0q";
	}

	@Override
	protected String expectedIndexName() {
		return "IDXpm66tdjkgtsca5x2uwux487t5";
	}
}
