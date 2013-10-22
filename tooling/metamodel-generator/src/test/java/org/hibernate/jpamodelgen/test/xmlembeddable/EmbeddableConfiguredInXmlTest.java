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
package org.hibernate.jpamodelgen.test.xmlembeddable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.TestForIssue;
import org.hibernate.jpamodelgen.test.util.TestUtil;

import static org.hibernate.jpamodelgen.test.util.TestUtil.getMetaModelSourceAsString;
import static org.testng.Assert.assertTrue;

/**
 * @author Hardy Ferentschik
 */
public class EmbeddableConfiguredInXmlTest extends CompilationTest {
	@Test
	@TestForIssue(jiraKey = "METAGEN-66")
	public void testAttributeForEmbeddableConfiguredInXmlExists() {
		// need to work with the source file. BusinessEntity_.class won't get generated, because the business id won't
		// be on the classpath
		String entityMetaModel = getMetaModelSourceAsString( BusinessEntity.class );
		assertTrue( entityMetaModel.contains( "SingularAttribute<BusinessEntity, BusinessId<T>> businessId" ) );
	}

	@Override
	@BeforeClass
	// override compileAllTestEntities to compile the the business id explicitly
	protected void compileAllTestEntities() throws Exception {
		String fooPackageName = getPackageNameOfCurrentTest() + ".foo";
		List<File> sourceFiles = getCompilationUnits(
				CompilationTest.getSourceBaseDir(), fooPackageName
		);
		compile( sourceFiles );

		sourceFiles = getCompilationUnits( getSourceBaseDir(), getPackageNameOfCurrentTest() );
		compile( sourceFiles );
	}

	@Override
	protected String getPackageNameOfCurrentTest() {
		return EmbeddableConfiguredInXmlTest.class.getPackage().getName();
	}

	@Override
	protected Collection<String> getOrmFiles() {
		List<String> ormFiles = new ArrayList<String>();
		String packageName = TestUtil.fcnToPath( EmbeddableConfiguredInXmlTest.class.getPackage().getName() );
		ormFiles.add( packageName + "/orm.xml" );
		return ormFiles;
	}
}
