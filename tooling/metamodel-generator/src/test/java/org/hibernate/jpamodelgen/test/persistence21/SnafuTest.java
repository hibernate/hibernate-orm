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
package org.hibernate.jpamodelgen.test.persistence21;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor;
import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.TestUtil;
import org.hibernate.jpamodelgen.test.xmlonly.Car;
import org.hibernate.jpamodelgen.test.xmlonly.Course;
import org.hibernate.jpamodelgen.test.xmlonly.Option;
import org.hibernate.jpamodelgen.test.xmlonly.Period;
import org.hibernate.jpamodelgen.test.xmlonly.Teacher;
import org.hibernate.jpamodelgen.test.xmlonly.Tire;
import org.testng.annotations.Test;

import static org.hibernate.jpamodelgen.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertPresenceOfFieldInMetamodelFor;

/**
 * @author Hardy Ferentschik
 */
public class SnafuTest extends CompilationTest {
	@Test
	public void testMetaModelGeneratedForXmlConfiguredEntity() {
		assertMetamodelClassGeneratedFor( Snafu.class );
	}

	@Override
	protected String getPackageNameOfCurrentTest() {
		return SnafuTest.class.getPackage().getName();
	}

	@Override
	protected Map<String, String> getProcessorOptions() {
		Map<String, String> properties = new HashMap<String, String>();
		properties.put(
				JPAMetaModelEntityProcessor.PERSISTENCE_XML_OPTION,
				TestUtil.fcnToPath( SnafuTest.class.getPackage().getName() ) + "/persistence.xml"
		);
		return properties;
	}
}