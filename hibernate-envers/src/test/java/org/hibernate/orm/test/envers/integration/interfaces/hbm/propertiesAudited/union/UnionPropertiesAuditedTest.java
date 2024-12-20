/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.interfaces.hbm.propertiesAudited.union;

import org.hibernate.orm.test.envers.integration.interfaces.hbm.propertiesAudited.AbstractPropertiesAuditedTest;

/**
 * @author Hern�n Chanfreau
 */
public class UnionPropertiesAuditedTest extends AbstractPropertiesAuditedTest {

	@Override
	protected String[] getMappings() {
		return new String[] {"mappings/interfaces/unionPropertiesAuditedMappings.hbm.xml"};
	}
}
