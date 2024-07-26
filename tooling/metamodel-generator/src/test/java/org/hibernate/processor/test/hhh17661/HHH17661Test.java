package org.hibernate.processor.test.hhh17661;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestUtil;
import org.hibernate.processor.test.util.WithClasses;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

@JiraKey(value = " HHH-17661")
public class HHH17661Test extends CompilationTest {

	@Test
	@WithClasses({ Entity.class, Tree.class, TreeRelation.class })
	@JiraKey(value = " HHH-17661")
	public void test() {
		System.out.println( TestUtil.getMetaModelSourceAsString( Entity.class ) );
		System.out.println( TestUtil.getMetaModelSourceAsString( Tree.class ) );
		System.out.println( TestUtil.getMetaModelSourceAsString( TreeRelation.class ) );
	}
}
