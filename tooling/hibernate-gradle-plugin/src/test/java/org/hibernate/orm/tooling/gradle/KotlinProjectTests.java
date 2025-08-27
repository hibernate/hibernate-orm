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
public class KotlinProjectTests extends TestsBase {

	@Override
	protected String getProjectName() {
		return "simple-kotlin";
	}

	@Override
	protected String getSourceSetName() {
		return "main";
	}

	@Override
	protected String getLanguageName() {
		return "kotlin";
	}

	@Override
	protected String getCompileTaskName() {
		return "compileKotlin";
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
