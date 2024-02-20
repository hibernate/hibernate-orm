package org.hibernate.jpamodelgen.test.hhh17661;

import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.TestForIssue;
import org.hibernate.jpamodelgen.test.util.TestUtil;
import org.hibernate.jpamodelgen.test.util.WithClasses;
import org.junit.Test;

@TestForIssue(jiraKey = " HHH-17661")
public class HHH17661Test extends CompilationTest {

	@Test
	@WithClasses({ Entity.class, Tree.class, TreeRelation.class })
	@TestForIssue(jiraKey = " HHH-17661")
	public void test() {
		System.out.println( TestUtil.getMetaModelSourceAsString( Entity.class ) );
		System.out.println( TestUtil.getMetaModelSourceAsString( Tree.class ) );
		System.out.println( TestUtil.getMetaModelSourceAsString( TreeRelation.class ) );
	}
}
