package org.hibernate.jpamodelgen.test.hhh17613;

import org.hibernate.jpamodelgen.test.hhh17613.a.ChildA;
import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.TestForIssue;
import org.hibernate.jpamodelgen.test.util.TestUtil;
import org.hibernate.jpamodelgen.test.util.WithClasses;

import org.junit.Test;

@TestForIssue(jiraKey = " HHH-17613")
public class HHH17613Test extends CompilationTest {

	@Test
	@WithClasses({ ChildA.class, ChildB.class, Parent.class })
	@TestForIssue(jiraKey = " HHH-17613")
	public void test() {
		System.out.println( TestUtil.getMetaModelSourceAsString( ChildA.class ) );
		System.out.println( TestUtil.getMetaModelSourceAsString( ChildB.class ) );
		System.out.println( TestUtil.getMetaModelSourceAsString( Parent.class ) );
	}
}
