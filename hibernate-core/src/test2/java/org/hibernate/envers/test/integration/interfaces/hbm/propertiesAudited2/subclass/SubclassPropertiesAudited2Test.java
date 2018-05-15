/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.interfaces.hbm.propertiesAudited2.subclass;

import org.hibernate.envers.test.integration.interfaces.hbm.propertiesAudited2.AbstractPropertiesAudited2Test;

/**
 * @author Hern�n Chanfreau
 */
public class SubclassPropertiesAudited2Test extends AbstractPropertiesAudited2Test {
	@Override
	protected String[] getMappings() {
		return new String[] {"mappings/interfaces/subclassPropertiesAudited2Mappings.hbm.xml"};
	}

}
