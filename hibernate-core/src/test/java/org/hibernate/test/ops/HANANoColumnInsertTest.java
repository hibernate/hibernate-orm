/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.ops;

import org.hibernate.MappingException;
import org.hibernate.dialect.AbstractHANADialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(value = { AbstractHANADialect.class })
public class HANANoColumnInsertTest extends BaseCoreFunctionalTestCase {

	public String[] getMappings() {
		return new String[]{
				"ops/Competition.hbm.xml"
		};
	}

	@Override
	protected void buildSessionFactory() {
		try {
			super.buildSessionFactory();

			fail( "Should have thrown MappingException!" );
		}
		catch (MappingException e) {
			assertEquals(
					"The INSERT statement for table [Competition] contains no column, and this is not supported by [" + getDialect().getClass().getName() + "]",
					e.getMessage() );
		}
	}

	@Test
	public void test() throws Exception {
	}
}
