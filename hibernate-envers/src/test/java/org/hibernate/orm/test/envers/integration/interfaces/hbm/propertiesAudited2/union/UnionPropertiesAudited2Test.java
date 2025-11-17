/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.interfaces.hbm.propertiesAudited2.union;

import org.hibernate.orm.test.envers.integration.interfaces.hbm.propertiesAudited2.AbstractPropertiesAudited2Test;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.Jpa;

/**
 * @author Hernán Chanfreau
 */
@EnversTest
@Jpa(xmlMappings = "mappings/interfaces/unionPropertiesAudited2Mappings.hbm.xml")
public class UnionPropertiesAudited2Test extends AbstractPropertiesAudited2Test {
	@Override
	protected String[] getMappings() {
		return new String[] {"mappings/interfaces/unionPropertiesAudited2Mappings.hbm.xml"};
	}
}
