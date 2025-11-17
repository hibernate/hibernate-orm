/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Steve Ebersole
 */
public class MultiPartNameTests extends TestsBase {

	@Override
	protected String getProjectName() {
		return "multi-part-source-set-name";
	}

	@Override
	protected String getSourceSetName() {
		return "mySpecialSourceSet";
	}

	@Override
	protected String getLanguageName() {
		return "java";
	}

	@Override
	protected String getCompileTaskName() {
		return "compileMySpecialSourceSetJava";
	}

	@Test
	@Override
	public void testEnhancement(@TempDir Path projectDir) throws Exception {
		super.testEnhancement( projectDir );
	}

	@Test
	public void testEnhancementUpToDate(@TempDir Path projectDir) throws Exception {
		super.testEnhancementUpToDate( projectDir );
	}
}
