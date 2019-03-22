/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.interfaces.hbm.propertiesAudited2;

import org.junit.jupiter.api.Disabled;

/**
 * @author Hern&aacute;n Chanfreau
 */
@Disabled("NYI - Joined inheritance support")
public class JoinedPropertiesAudited2Test extends AbstractPropertiesAudited2Test {
	@Override
	protected String[] getMappings() {
		return new String[] { "interfaces/joinedPropertiesAudited2Mappings.hbm.xml" };
	}
}
