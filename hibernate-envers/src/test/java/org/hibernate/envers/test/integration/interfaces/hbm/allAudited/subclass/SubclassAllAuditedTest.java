package org.hibernate.envers.test.integration.interfaces.hbm.allAudited.subclass;

import org.hibernate.envers.test.integration.interfaces.hbm.allAudited.AbstractAllAuditedTest;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;

/**
 * @author Hern�n Chanfreau
 */
@FailureExpectedWithNewMetamodel( message = "hbm.xml source not supported because it is not indexed." )
public class SubclassAllAuditedTest extends AbstractAllAuditedTest {
	@Override
	protected String[] getMappings() {
		return new String[] {"mappings/interfaces/subclassAllAuditedMappings.hbm.xml"};
	}
}
