package org.hibernate.orm.test.envers.integration.interfaces.hbm.allAudited.joined;

import org.hibernate.orm.test.envers.integration.interfaces.hbm.allAudited.AbstractAllAuditedTest;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.Jpa;

/**
 * @author Hernán Chanfreau
 */
@EnversTest
@Jpa(xmlMappings = "mappings/interfaces/joinedAllAuditedMappings.hbm.xml")
public class JoinedAllAuditedTest extends AbstractAllAuditedTest {
}
