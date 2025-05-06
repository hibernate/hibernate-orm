/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.interfaces.hbm.allAudited.subclass;

import org.hibernate.orm.test.envers.integration.interfaces.hbm.allAudited.AbstractAllAuditedTest;

/**
 * @author Hern�n Chanfreau
 */
public class SubclassAllAuditedTest extends AbstractAllAuditedTest {
	@Override
	protected String[] getMappings() {
		return new String[] {"mappings/interfaces/subclassAllAuditedMappings.hbm.xml"};
	}
}
