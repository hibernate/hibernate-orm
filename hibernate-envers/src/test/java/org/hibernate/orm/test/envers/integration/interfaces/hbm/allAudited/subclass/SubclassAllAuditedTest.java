/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.interfaces.hbm.allAudited.subclass;

import org.hibernate.orm.test.envers.integration.interfaces.hbm.allAudited.AbstractAllAuditedTest;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.Jpa;

/**
 * @author Hernán Chanfreau
 */
@EnversTest
@Jpa(xmlMappings = "mappings/interfaces/subclassAllAuditedMappings.hbm.xml")
public class SubclassAllAuditedTest extends AbstractAllAuditedTest {
}
