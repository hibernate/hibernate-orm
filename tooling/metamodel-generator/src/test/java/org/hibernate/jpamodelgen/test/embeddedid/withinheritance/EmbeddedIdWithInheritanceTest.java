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
package org.hibernate.jpamodelgen.test.embeddedid.withinheritance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.TestUtil;
import org.testng.annotations.Test;

import static org.hibernate.jpamodelgen.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertPresenceOfFieldInMetamodelFor;

/**
 * @author Hardy Ferentschik
 */
public class EmbeddedIdWithInheritanceTest extends CompilationTest {
	@Test
	public void testEntityContainsEmbeddedIdProperty() {
		assertMetamodelClassGeneratedFor( TestEntity.class );
		assertPresenceOfFieldInMetamodelFor(
				TestEntity.class, "ref", "Property ref should be in metamodel"
		);
	}

	@Override
	protected String getPackageNameOfCurrentTest() {
		return EmbeddedIdWithInheritanceTest.class.getPackage().getName();
	}

	@Override
	protected Collection<String> getOrmFiles() {
		List<String> ormFiles = new ArrayList<String>();
		String dir = TestUtil.fcnToPath( EmbeddedIdWithInheritanceTest.class.getPackage().getName() );
		ormFiles.add( dir + "/orm.xml" );
		return ormFiles;
	}
}
