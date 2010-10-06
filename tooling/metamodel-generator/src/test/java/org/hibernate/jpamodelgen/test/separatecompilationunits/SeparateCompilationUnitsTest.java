/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// $Id$

package org.hibernate.jpamodelgen.test.separatecompilationunits;

import java.io.File;
import java.util.List;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.hibernate.jpamodelgen.test.util.CompilationTest;

import static org.hibernate.jpamodelgen.test.util.TestUtil.getMetaModelSourceAsString;
import static org.testng.Assert.assertTrue;

/**
 * @author Hardy Ferentschik
 * @see METAGEN-35
 */
public class SeparateCompilationUnitsTest extends CompilationTest {
	@Test
	public void testInheritance() throws Exception {
		// need to work with the source file. Entity_.class won't get generated, because the mapped superclass
		// will not be on the classpath
		String entityMetaModel = getMetaModelSourceAsString( Entity.class );
		assertTrue(
				entityMetaModel.contains(
						"extends org.hibernate.jpamodelgen.test.separatecompilationunits.superclass.MappedSuperclass"
				)
		);
	}

	@Override
	@BeforeClass
	// override compileAllTestEntities to compile the mapped super class explicitly
	protected void compileAllTestEntities() throws Exception {
		String superClassPackageName = getPackageNameOfCurrentTest() + ".superclass";
		List<File> sourceFiles = getCompilationUnits(
				CompilationTest.getSourceBaseDir(), superClassPackageName
		);
		compile( sourceFiles, superClassPackageName );

		sourceFiles = getCompilationUnits( getSourceBaseDir(), getPackageNameOfCurrentTest() );
		compile( sourceFiles, getPackageNameOfCurrentTest() );
	}

	@Override
	protected String getPackageNameOfCurrentTest() {
		return SeparateCompilationUnitsTest.class.getPackage().getName();
	}
}
