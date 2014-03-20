package org.hibernate.envers.test.integration.interfaces.hbm.propertiesAudited2.joined;

import org.hibernate.envers.test.integration.interfaces.hbm.propertiesAudited2.AbstractPropertiesAudited2Test;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;

/**
 * @author Hern�n Chanfreau
 */
@FailureExpectedWithNewMetamodel( message = "hbm.xml source not supported because it is not indexed." )
public class JoinedPropertiesAudited2Test extends AbstractPropertiesAudited2Test {
	@Override
	protected String[] getMappings() {
		return new String[] {"mappings/interfaces/joinedPropertiesAudited2Mappings.hbm.xml"};
	}
}
