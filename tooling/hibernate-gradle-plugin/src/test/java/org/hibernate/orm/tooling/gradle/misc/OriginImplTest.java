/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle.misc;

import org.hibernate.boot.jaxb.SourceType;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class OriginImplTest {

	@Test
	public void testConstructorAndGetters() {
		File hbmFile = new File("/tmp/com/example/Person.hbm.xml");
		OriginImpl origin = new OriginImpl(hbmFile);

		assertEquals(hbmFile, origin.getHbmXmlFile());
		assertEquals(SourceType.FILE, origin.getType());
		assertEquals(hbmFile.getAbsolutePath(), origin.getName());
	}

	@Test
	public void testToString() {
		File hbmFile = new File("/tmp/Person.hbm.xml");
		OriginImpl origin = new OriginImpl(hbmFile);
		assertNotNull(origin.toString());
	}
}
