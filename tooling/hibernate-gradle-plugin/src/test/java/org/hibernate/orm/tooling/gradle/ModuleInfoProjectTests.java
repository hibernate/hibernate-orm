/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
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
class ModuleInfoProjectTests extends TestsBase {

	@Override
	protected String getProjectName() {
		return "simple-moduleinfo";
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
