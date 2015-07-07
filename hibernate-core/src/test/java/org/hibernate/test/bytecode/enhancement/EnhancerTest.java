/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.test.bytecode.enhancement.association.ManyToManyAssociationTestTask;
import org.hibernate.test.bytecode.enhancement.association.OneToManyAssociationTestTask;
import org.hibernate.test.bytecode.enhancement.association.OneToOneAssociationTestTask;
import org.hibernate.test.bytecode.enhancement.basic.BasicEnhancementTestTask;
import org.hibernate.test.bytecode.enhancement.dirty.DirtyTrackingTestTask;
import org.hibernate.test.bytecode.enhancement.join.HHH3949TestTask1;
import org.hibernate.test.bytecode.enhancement.join.HHH3949TestTask2;
import org.hibernate.test.bytecode.enhancement.join.HHH3949TestTask3;
import org.hibernate.test.bytecode.enhancement.join.HHH3949TestTask4;
import org.hibernate.test.bytecode.enhancement.lazy.LazyLoadingIntegrationTestTask;
import org.hibernate.test.bytecode.enhancement.lazy.LazyLoadingTestTask;
import org.junit.Test;

/**
 * @author Luis Barreiro
 */
public class EnhancerTest extends BaseUnitTestCase {

	@Test
	public void testBasic() {
		EnhancerTestUtils.runEnhancerTestTask( BasicEnhancementTestTask.class );
	}

	@Test
	public void testDirty() {
		EnhancerTestUtils.runEnhancerTestTask( DirtyTrackingTestTask.class );
	}

	@Test
	public void testAssociation() {
		EnhancerTestUtils.runEnhancerTestTask( OneToOneAssociationTestTask.class );
		EnhancerTestUtils.runEnhancerTestTask( OneToManyAssociationTestTask.class );
		EnhancerTestUtils.runEnhancerTestTask( ManyToManyAssociationTestTask.class );
	}

	@Test
	public void testLazy() {
		EnhancerTestUtils.runEnhancerTestTask( LazyLoadingTestTask.class );
		EnhancerTestUtils.runEnhancerTestTask( LazyLoadingIntegrationTestTask.class );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-3949" )
	@FailureExpected( jiraKey = "HHH-3949" )
	public void testJoinFetchLazyToOneAttributeHql() {
		EnhancerTestUtils.runEnhancerTestTask( HHH3949TestTask1.class );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-3949" )
	@FailureExpected( jiraKey = "HHH-3949" )
	public void testJoinFetchLazyToOneAttributeHql2() {
		EnhancerTestUtils.runEnhancerTestTask( HHH3949TestTask2.class );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-3949" )
	@FailureExpected( jiraKey = "HHH-3949" )
	public void testHHH3949() {
		EnhancerTestUtils.runEnhancerTestTask( HHH3949TestTask3.class );
		EnhancerTestUtils.runEnhancerTestTask( HHH3949TestTask4.class );
	}

}
