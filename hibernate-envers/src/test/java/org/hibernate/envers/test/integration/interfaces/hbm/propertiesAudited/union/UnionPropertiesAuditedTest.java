package org.hibernate.envers.test.integration.interfaces.hbm.propertiesAudited.union;

import org.hibernate.envers.test.integration.interfaces.hbm.propertiesAudited.AbstractPropertiesAuditedTest;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;

/**
 * @author Hern�n Chanfreau
 */
@FailureExpectedWithNewMetamodel( message = "hbm.xml source not supported because it is not indexed." )
public class UnionPropertiesAuditedTest extends AbstractPropertiesAuditedTest {

	@Override
	protected String[] getMappings() {
		return new String[] {"mappings/interfaces/unionPropertiesAuditedMappings.hbm.xml"};
	}
}
