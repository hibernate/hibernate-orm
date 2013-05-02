package org.hibernate.envers.test.integration.interfaces.hbm.propertiesAudited2.union;

import org.hibernate.envers.test.integration.interfaces.hbm.propertiesAudited2.AbstractPropertiesAudited2Test;

/**
 * @author Hern�n Chanfreau
 */
public class UnionPropertiesAudited2Test extends AbstractPropertiesAudited2Test {
	@Override
	protected String[] getMappings() {
		return new String[] {"mappings/interfaces/unionPropertiesAudited2Mappings.hbm.xml"};
	}
}
