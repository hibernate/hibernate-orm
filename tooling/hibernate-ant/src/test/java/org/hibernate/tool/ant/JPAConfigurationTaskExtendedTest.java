/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant;

import org.apache.tools.ant.BuildException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JPAConfigurationTaskExtendedTest {

	@Test
	public void testCreateMetadataDescriptorWithNoPersistenceUnit() {
		JPAConfigurationTask task = new JPAConfigurationTask();
		// With no persistence unit and no META-INF/persistence.xml, this should throw
		BuildException ex = assertThrows(BuildException.class, task::createMetadataDescriptor);
		assertNotNull(ex);
	}

	@Test
	public void testCreateMetadataDescriptorWithInvalidPersistenceUnit() {
		JPAConfigurationTask task = new JPAConfigurationTask();
		task.setPersistenceUnit("nonExistentPU");
		BuildException ex = assertThrows(BuildException.class, task::createMetadataDescriptor);
		assertNotNull(ex);
	}

	@Test
	public void testSetConfigurationFileThrowsWithMessage() {
		JPAConfigurationTask task = new JPAConfigurationTask();
		BuildException ex = assertThrows(BuildException.class,
				() -> task.setConfigurationFile(new java.io.File("/tmp/cfg.xml")));
		assertTrue(ex.getMessage().contains("autodiscovery"));
	}
}
