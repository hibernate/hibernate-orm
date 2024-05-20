package org.hibernate.jpamodelgen.test.map;

import org.hibernate.jpamodelgen.test.util
		.CompilationTest;
import org.hibernate.jpamodelgen.test.util.TestForIssue;
import org.hibernate.jpamodelgen.test.util.WithClasses;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static org.hibernate.jpamodelgen.test.util.TestUtil.getMetamodelClassFor;

public class MetamodelGeneratedTest extends CompilationTest {

	@Test
	@WithClasses({ MapOfMapEntity.class })
	@TestForIssue(jiraKey = " HHH-17514")
	public void test() {
		Class<?> repositoryClass = getMetamodelClassFor( MapOfMapEntity.class );
		Assertions.assertNotNull( repositoryClass );
	}
}
