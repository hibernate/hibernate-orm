package org.hibernate.envers.test.integration.interfaces.hbm.propertiesAudited.joined;

import org.hibernate.envers.test.integration.interfaces.hbm.propertiesAudited.AbstractPropertiesAuditedTest;

/**
 * @author Hern�n Chanfreau
 */
public class JoinedPropertiesAuditedTest extends AbstractPropertiesAuditedTest {

	@Override
	protected String[] getMappings() {
		return new String[] {"mappings/interfaces/joinedPropertiesAuditedMappings.hbm.xml"};
	}
}
