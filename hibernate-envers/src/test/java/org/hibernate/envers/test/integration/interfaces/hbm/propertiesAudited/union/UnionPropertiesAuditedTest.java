package org.hibernate.envers.test.integration.interfaces.hbm.propertiesAudited.union;

import org.hibernate.envers.test.integration.interfaces.hbm.propertiesAudited.AbstractPropertiesAuditedTest;

/**
 * @author Hern�n Chanfreau
 */
public class UnionPropertiesAuditedTest extends AbstractPropertiesAuditedTest {

	@Override
	protected String[] getMappings() {
		return new String[] {"mappings/interfaces/unionPropertiesAuditedMappings.hbm.xml"};
	}
}
