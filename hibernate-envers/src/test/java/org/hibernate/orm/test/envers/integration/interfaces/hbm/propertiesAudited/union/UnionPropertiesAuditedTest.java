package org.hibernate.orm.test.envers.integration.interfaces.hbm.propertiesAudited.union;

import org.hibernate.orm.test.envers.integration.interfaces.hbm.propertiesAudited.AbstractPropertiesAuditedTest;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.Jpa;

/**
 * @author Hernán Chanfreau
 */
@EnversTest
@Jpa(xmlMappings = "mappings/interfaces/unionPropertiesAuditedMappings.hbm.xml")
public class UnionPropertiesAuditedTest extends AbstractPropertiesAuditedTest {
}
