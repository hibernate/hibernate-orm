package org.hibernate.envers.test.integration.interfaces.hbm.allAudited.joined;

import org.hibernate.envers.test.integration.interfaces.hbm.allAudited.AbstractAllAuditedTest;

/**
 * @author Hern�n Chanfreau
 */
public class JoinedAllAuditedTest extends AbstractAllAuditedTest {
    @Override
    protected String[] getMappings() {
        return new String[]{"mappings/interfaces/joinedAllAuditedMappings.hbm.xml"};
    }
}
