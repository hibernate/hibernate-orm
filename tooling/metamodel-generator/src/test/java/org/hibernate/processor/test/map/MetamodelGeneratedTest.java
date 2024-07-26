package org.hibernate.processor.test.map;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static org.hibernate.processor.test.util.TestUtil.getMetamodelClassFor;

public class MetamodelGeneratedTest extends CompilationTest {

	@Test
	@WithClasses({ MapOfMapEntity.class })
	@JiraKey(value = " HHH-17514")
	public void test() {
		Class<?> repositoryClass = getMetamodelClassFor( MapOfMapEntity.class );
		Assertions.assertNotNull( repositoryClass );
	}
}
