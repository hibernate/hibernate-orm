package org.hibernate.envers.test.integration.interfaces.hbm.propertiesAudited.joined;

import org.hibernate.envers.test.integration.interfaces.hbm.propertiesAudited.AbstractPropertiesAuditedTest;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;

/**
 * @author Hern�n Chanfreau
 */
@FailureExpectedWithNewMetamodel( message = "hbm.xml source not supported because it is not indexed." )
public class JoinedPropertiesAuditedTest extends AbstractPropertiesAuditedTest {

	@Override
	protected String[] getMappings() {
		return new String[] {"mappings/interfaces/joinedPropertiesAuditedMappings.hbm.xml"};
	}
}
