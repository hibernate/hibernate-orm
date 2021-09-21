/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.util;

import org.junit.After;
import org.junit.runner.RunWith;

/**
 * Base class for annotation processor tests.
 *
 * @author Hardy Ferentschik
 */
@RunWith(CompilationRunner.class)
public abstract class CompilationTest {

	public CompilationTest() {
	}

	@After
	public void cleanup() throws Exception {
		TestUtil.deleteProcessorGeneratedFiles();
	}
}


