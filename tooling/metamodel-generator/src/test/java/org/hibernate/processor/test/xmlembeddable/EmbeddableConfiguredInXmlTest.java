/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.xmlembeddable;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.IgnoreCompilationErrors;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.WithClasses;
import org.hibernate.processor.test.util.WithMappingFiles;
import org.hibernate.processor.test.xmlembeddable.foo.BusinessId;

import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
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
