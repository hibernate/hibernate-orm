/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.interfaces.hbm.allAudited.union;

import org.hibernate.orm.test.envers.integration.interfaces.hbm.allAudited.AbstractAllAuditedTest;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.Jpa;

/**
 * @author Hernán Chanfreau
 */
@EnversTest
@Jpa(xmlMappings = "mappings/interfaces/unionAllAuditedMappings.hbm.xml")
public class UnionAllAuditedTest extends AbstractAllAuditedTest {
	protected String[] getMappings() {
		return new String[] {"mappings/interfaces/unionAllAuditedMappings.hbm.xml"};
	}
}
