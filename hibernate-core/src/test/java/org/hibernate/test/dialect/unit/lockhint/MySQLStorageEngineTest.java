/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.dialect.unit.lockhint;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.MySQL57Dialect;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Vlad Mihalcea
 */
public class MySQLStorageEngineTest extends BaseUnitTestCase {

	@Test
	public void testDefaultStorage() {
		assertEquals( " engine=InnoDB", new MySQL57Dialect().getTableTypeString() );
	}

	@Test
	public void testOverrideStorage() {
		String previousValue = System.setProperty( AvailableSettings.STORAGE_ENGINE, "myisam" );
		try {
			assertEquals( " engine=MyISAM", new MySQL57Dialect().getTableTypeString() );
		}
		finally {
			if ( previousValue != null ) {
				System.setProperty( AvailableSettings.STORAGE_ENGINE, previousValue );
			}
			else {
				System.clearProperty( AvailableSettings.STORAGE_ENGINE );
				assertThat( System.getProperty( AvailableSettings.STORAGE_ENGINE ), is( nullValue() ) );
			}
		}
	}
}
