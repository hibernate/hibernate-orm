/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;


/**
 * Basic functional tests
 *
 * @author Steve Ebersole
 */
class JavaProjectTests extends TestsBase {

	@Override
	protected String getProjectName() {
		return "simple";
	}

	@Override
	protected String getSourceSetName() {
		return "main";
	}

	@Override
	protected String getLanguageName() {
		return "java";
	}

	@Override
	protected String getCompileTaskName() {
		return "compileJava";
	}

	@Test
	@Override
	public void testEnhancement(@TempDir Path projectDir) throws Exception {
		super.testEnhancement( projectDir );
	}

	@Test
	@Override
	public void testEnhancementUpToDate(@TempDir Path projectDir) throws Exception {
		super.testEnhancementUpToDate( projectDir );
	}
}
