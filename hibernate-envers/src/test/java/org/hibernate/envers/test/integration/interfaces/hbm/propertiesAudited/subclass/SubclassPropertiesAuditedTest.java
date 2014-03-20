package org.hibernate.envers.test.integration.interfaces.hbm.propertiesAudited.subclass;

import org.hibernate.envers.test.integration.interfaces.hbm.propertiesAudited.AbstractPropertiesAuditedTest;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;

/**
 * @author Hern�n Chanfreau
 */
@FailureExpectedWithNewMetamodel( message = "hbm.xml source not supported because it is not indexed." )
public class SubclassPropertiesAuditedTest extends AbstractPropertiesAuditedTest {
	@Override
	protected String[] getMappings() {
		return new String[] {"mappings/interfaces/subclassPropertiesAuditedMappings.hbm.xml"};
	}
}
