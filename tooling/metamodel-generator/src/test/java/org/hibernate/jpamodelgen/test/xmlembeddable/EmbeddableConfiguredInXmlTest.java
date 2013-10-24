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

import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.IgnoreCompilationErrors;
import org.hibernate.jpamodelgen.test.util.TestForIssue;
import org.hibernate.jpamodelgen.test.util.WithClasses;
import org.hibernate.jpamodelgen.test.util.WithMappingFiles;
import org.hibernate.jpamodelgen.test.xmlembeddable.foo.BusinessId;
import org.junit.Test;

import static org.hibernate.jpamodelgen.test.util.TestUtil.getMetaModelSourceAsString;
import static org.junit.Assert.assertTrue;

/**
 * @author Hardy Ferentschik
 */
public class EmbeddableConfiguredInXmlTest extends CompilationTest {
	@Test
	@TestForIssue(jiraKey = "METAGEN-66")
	@WithClasses(value = { Foo.class, BusinessEntity.class }, preCompile = BusinessId.class)
	@WithMappingFiles("orm.xml")
	@IgnoreCompilationErrors
	public void testAttributeForEmbeddableConfiguredInXmlExists() {
		// need to work with the source file. BusinessEntity_.class won't get generated, because the business id won't
		// be on the classpath
		String entityMetaModel = getMetaModelSourceAsString( BusinessEntity.class );
		assertTrue( entityMetaModel.contains( "SingularAttribute<BusinessEntity, BusinessId<T>> businessId" ) );
	}
}
