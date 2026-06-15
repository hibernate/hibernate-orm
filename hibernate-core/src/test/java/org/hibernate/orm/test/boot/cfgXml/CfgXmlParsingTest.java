/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.cfgXml;

import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class CfgXmlParsingTest {
	@Test
	public void testCfgXmlIsUnsupported() {
		assertThrows(
				UnsupportedOperationException.class,
				() -> ServiceRegistryUtil.serviceRegistryBuilder()
						.configure( "org/hibernate/orm/test/boot/cfgXml/hibernate.cfg.xml" )
		);
	}

	@Test
	public void testBadNamespaceCfgXmlIsUnsupported() {
		assertThrows(
				UnsupportedOperationException.class,
				() -> ServiceRegistryUtil.serviceRegistryBuilder()
						.configure( "org/hibernate/orm/test/boot/cfgXml/badnamespace.cfg.xml" )
		);
	}
}
