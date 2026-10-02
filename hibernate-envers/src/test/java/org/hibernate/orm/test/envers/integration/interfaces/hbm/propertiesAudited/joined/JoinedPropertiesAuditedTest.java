package org.hibernate.orm.test.envers.integration.interfaces.hbm.propertiesAudited.joined;

import org.hibernate.orm.test.envers.integration.interfaces.hbm.propertiesAudited.AbstractPropertiesAuditedTest;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.Jpa;

/**
 * @author Hernán Chanfreau
 */
@EnversTest
@Jpa(xmlMappings = "mappings/interfaces/joinedPropertiesAuditedMappings.hbm.xml")
public class JoinedPropertiesAuditedTest extends AbstractPropertiesAuditedTest {
}
