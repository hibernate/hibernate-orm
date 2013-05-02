package org.hibernate.envers.test.integration.interfaces.hbm.propertiesAudited2.joined;

import org.hibernate.envers.test.integration.interfaces.hbm.propertiesAudited2.AbstractPropertiesAudited2Test;

/**
 * @author Hern�n Chanfreau
 */
public class JoinedPropertiesAudited2Test extends AbstractPropertiesAudited2Test {
	@Override
	protected String[] getMappings() {
		return new String[] {"mappings/interfaces/joinedPropertiesAudited2Mappings.hbm.xml"};
	}
}
