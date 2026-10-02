package org.hibernate.orm.test.envers.integration.interfaces.hbm.propertiesAudited.subclass;

import org.hibernate.orm.test.envers.integration.interfaces.hbm.propertiesAudited.AbstractPropertiesAuditedTest;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.Jpa;

/**
 * @author Hernán Chanfreau
 */
@EnversTest
@Jpa(xmlMappings = "mappings/interfaces/subclassPropertiesAuditedMappings.hbm.xml")
public class SubclassPropertiesAuditedTest extends AbstractPropertiesAuditedTest {
}
