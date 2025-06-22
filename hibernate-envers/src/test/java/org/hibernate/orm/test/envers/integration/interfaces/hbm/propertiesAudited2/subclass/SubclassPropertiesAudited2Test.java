/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.interfaces.hbm.propertiesAudited2.subclass;

import org.hibernate.orm.test.envers.integration.interfaces.hbm.propertiesAudited2.AbstractPropertiesAudited2Test;

/**
 * @author Hern�n Chanfreau
 */
public class SubclassPropertiesAudited2Test extends AbstractPropertiesAudited2Test {
	@Override
	protected String[] getMappings() {
		return new String[] {"mappings/interfaces/subclassPropertiesAudited2Mappings.hbm.xml"};
	}

}
