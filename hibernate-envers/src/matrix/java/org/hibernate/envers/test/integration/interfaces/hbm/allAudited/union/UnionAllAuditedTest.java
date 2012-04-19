package org.hibernate.envers.test.integration.interfaces.hbm.allAudited.union;

import org.hibernate.envers.test.integration.interfaces.hbm.allAudited.AbstractAllAuditedTest;

/**
 * @author Hern�n Chanfreau
 */
public class UnionAllAuditedTest extends AbstractAllAuditedTest {
    @Override
    protected String[] getMappings() {
        return new String[]{"mappings/interfaces/unionAllAuditedMappings.hbm.xml"};
    }
}
