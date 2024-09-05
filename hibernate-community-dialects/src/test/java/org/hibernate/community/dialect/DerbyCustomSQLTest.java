/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;

import org.hibernate.orm.test.sql.hand.custom.CustomStoredProcTestSupport;

import org.hibernate.testing.orm.junit.RequiresDialect;


/**
 * @author Andrea Boriero
 */
@RequiresDialect(DerbyDialect.class)
public class DerbyCustomSQLTest extends CustomStoredProcTestSupport {
	public String[] getMappings() {
		return new String[] {"derby/Mappings.hbm.xml"};
	}
}
