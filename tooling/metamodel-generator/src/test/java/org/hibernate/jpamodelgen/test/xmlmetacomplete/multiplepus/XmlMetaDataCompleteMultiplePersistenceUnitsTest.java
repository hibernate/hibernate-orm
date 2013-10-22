/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc. and individual contributors
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
package org.hibernate.jpamodelgen.test.xmlmetacomplete.multiplepus;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor;
import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.TestForIssue;
import org.hibernate.jpamodelgen.test.util.TestUtil;
import org.testng.annotations.Test;

import static org.hibernate.jpamodelgen.test.util.TestUtil.assertMetamodelClassGeneratedFor;

/**
 * @author Hardy Ferentschik
 */
@TestForIssue( jiraKey = "METAGEN-86" )
public class XmlMetaDataCompleteMultiplePersistenceUnitsTest extends CompilationTest {

	@Test
	public void testMetaModelGenerated() {
		// only one of the xml files in the example uses 'xml-mapping-metadata-complete', hence annotation processing
		// kicks in
		assertMetamodelClassGeneratedFor( Dummy.class );
	}

	@Override
	protected String getPackageNameOfCurrentTest() {
		return XmlMetaDataCompleteMultiplePersistenceUnitsTest.class.getPackage().getName();
	}

	@Override
	protected Map<String, String> getProcessorOptions() {
		Map<String, String> properties = new HashMap<String, String>();
		properties.put(
				JPAMetaModelEntityProcessor.PERSISTENCE_XML_OPTION,
				TestUtil.fcnToPath(
						XmlMetaDataCompleteMultiplePersistenceUnitsTest.class.getPackage().getName()
				) + "/persistence.xml"
		);
		return properties;
	}
}

