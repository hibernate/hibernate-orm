/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.sql.hand.custom.derby;

import org.hibernate.dialect.DerbyDialect;
import org.hibernate.orm.test.sql.hand.custom.CustomStoredProcTestSupport;

import org.hibernate.testing.RequiresDialect;
import org.junit.Ignore;

/**
 * @author Andrea Boriero
 */
@RequiresDialect(DerbyDialect.class)
// todo (6.0): needs a composite user type mechanism e.g. by providing a custom embeddable strategy or istantiator
@Ignore( "Missing support for composite user types" )
public class DerbyCustomSQLTest extends CustomStoredProcTestSupport {
	public String[] getMappings() {
		return new String[] {"sql/hand/custom/derby/Mappings.hbm.xml"};
	}
}
