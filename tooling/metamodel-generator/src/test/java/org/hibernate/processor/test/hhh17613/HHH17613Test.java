/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.hhh17613;

import org.hibernate.processor.test.hhh17613.a.ChildA;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.TestUtil;
import org.hibernate.processor.test.util.WithClasses;

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
