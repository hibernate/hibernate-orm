package org.hibernate.envers.test.integration.interfaces.hbm.propertiesAudited2.subclass;

import org.hibernate.envers.test.integration.interfaces.hbm.propertiesAudited2.AbstractPropertiesAudited2Test;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;

/**
 * @author Hern�n Chanfreau
 */
@FailureExpectedWithNewMetamodel( message = "hbm.xml source not supported because it is not indexed." )
public class SubclassPropertiesAudited2Test extends AbstractPropertiesAudited2Test {
	@Override
	protected String[] getMappings() {
		return new String[] {"mappings/interfaces/subclassPropertiesAudited2Mappings.hbm.xml"};
	}

}
