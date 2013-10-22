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
package org.hibernate.jpamodelgen.test.sortedcollection;

import org.testng.annotations.Test;

import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.TestForIssue;

import static org.hibernate.jpamodelgen.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertPresenceOfFieldInMetamodelFor;

/**
 * @author Hardy Ferentschik
 */
public class SortedCollectionTest extends CompilationTest {

	@Test
	@TestForIssue(jiraKey = "METAGEN-62")
	public void testGenerics() {
		assertMetamodelClassGeneratedFor( Printer.class );
		assertMetamodelClassGeneratedFor( PrintJob.class );
		assertPresenceOfFieldInMetamodelFor( Printer.class, "printQueue", "There sorted set attribute is missing" );
		assertPresenceOfFieldInMetamodelFor( Printer.class, "printedJobs", "There sorted map attribute is missing" );
	}

	@Override
	protected String getPackageNameOfCurrentTest() {
		return SortedCollectionTest.class.getPackage().getName();
	}
}
