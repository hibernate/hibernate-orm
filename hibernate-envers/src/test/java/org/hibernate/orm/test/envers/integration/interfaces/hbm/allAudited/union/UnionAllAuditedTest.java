/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.interfaces.hbm.allAudited.union;

import org.hibernate.orm.test.envers.integration.interfaces.hbm.allAudited.AbstractAllAuditedTest;

/**
 * @author Hern�n Chanfreau
 */
public class UnionAllAuditedTest extends AbstractAllAuditedTest {
	@Override
	protected String[] getMappings() {
		return new String[] {"mappings/interfaces/unionAllAuditedMappings.hbm.xml"};
	}
}
