/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hqlfetchscroll;

import org.hibernate.dialect.AbstractHANADialect;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.testing.RequiresDialect;

@RequiresDialect( value = { Oracle8iDialect.class, AbstractHANADialect.class },
		comment = "Oracle/HANA do not support the identity column used in the HQLScrollFetchTest mapping." )
public class NoIdentityHQLScrollFetchTest extends HQLScrollFetchTest {

	@Override
	public String[] getMappings() {
		return new String[] { "hqlfetchscroll/NoIdentityParentChild.hbm.xml" };
	}
}
