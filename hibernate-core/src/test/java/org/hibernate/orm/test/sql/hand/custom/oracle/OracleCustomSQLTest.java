/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.sql.hand.custom.oracle;

import org.hibernate.dialect.OracleDialect;
import org.hibernate.orm.test.sql.hand.custom.CustomStoredProcTestSupport;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.NotImplementedYet;
import org.junit.Ignore;

/**
 * Custom SQL tests for Oracle
 * 
 * @author Gavin King
 */
@RequiresDialect( OracleDialect.class )
// todo (6.0): needs a composite user type mechanism e.g. by providing a custom embeddable strategy or istantiator
@Ignore( "Missing support for composite user types" )
@NotImplementedYet
public class OracleCustomSQLTest extends CustomStoredProcTestSupport {
	public String[] getMappings() {
		return new String[] { "sql/hand/custom/oracle/Mappings.hbm.xml", "sql/hand/custom/oracle/StoredProcedures.hbm.xml" };
	}
}

