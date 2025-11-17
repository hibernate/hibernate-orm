/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tool.schema.scripts;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.SchemaToolingSettings.JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey("HHH-13089")
@ServiceRegistry(settings = @Setting( name = JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE, value = ""))
@SessionFactory
public class EmptyImportFilesTest {
	@Test
	public void testImportFile(SessionFactoryScope factoryScope) {
		factoryScope.getSessionFactory();
	}
}
