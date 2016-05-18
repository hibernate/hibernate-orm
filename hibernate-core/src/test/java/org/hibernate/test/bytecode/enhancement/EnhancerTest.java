/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.test.bytecode.enhancement.association.ManyToManyAssociationTestTask;
import org.hibernate.test.bytecode.enhancement.association.OneToManyAssociationTestTask;
import org.hibernate.test.bytecode.enhancement.association.OneToOneAssociationTestTask;
import org.hibernate.test.bytecode.enhancement.basic.BasicEnhancementTestTask;
import org.hibernate.test.bytecode.enhancement.basic.HHH9529TestTask;
import org.hibernate.test.bytecode.enhancement.cascade.CascadeDeleteTestTask;
import org.hibernate.test.bytecode.enhancement.dirty.DirtyTrackingTestTask;
import org.hibernate.test.bytecode.enhancement.extended.ExtendedAssociationManagementTestTasK;
import org.hibernate.test.bytecode.enhancement.extended.ExtendedEnhancementTestTask;
import org.hibernate.test.bytecode.enhancement.join.HHH3949TestTask1;
import org.hibernate.test.bytecode.enhancement.join.HHH3949TestTask2;
import org.hibernate.test.bytecode.enhancement.join.HHH3949TestTask3;
import org.hibernate.test.bytecode.enhancement.join.HHH3949TestTask4;
import org.hibernate.test.bytecode.enhancement.lazy.HHH_10708.UnexpectedDeleteOneTestTask;
import org.hibernate.test.bytecode.enhancement.lazy.HHH_10708.UnexpectedDeleteTwoTestTask;
import org.hibernate.test.bytecode.enhancement.lazy.LazyBasicFieldNotInitializedTestTask;
import org.hibernate.test.bytecode.enhancement.lazy.LazyCollectionLoadingTestTask;
import org.hibernate.test.bytecode.enhancement.lazy.LazyLoadingIntegrationTestTask;
import org.hibernate.test.bytecode.enhancement.lazy.LazyLoadingTestTask;
import org.hibernate.test.bytecode.enhancement.lazy.basic.LazyBasicFieldAccessTestTask;
import org.hibernate.test.bytecode.enhancement.lazy.basic.LazyBasicPropertyAccessTestTask;
import org.hibernate.test.bytecode.enhancement.lazy.group.LazyGroupAccessTestTask;
import org.hibernate.test.bytecode.enhancement.lazyCache.InitFromCacheTestTask;
import org.hibernate.test.bytecode.enhancement.mapped.MappedSuperclassTestTask;
import org.hibernate.test.bytecode.enhancement.merge.CompositeMergeTestTask;
import org.hibernate.test.bytecode.enhancement.ondemandload.LazyCollectionWithClearedSessionTestTask;
import org.hibernate.test.bytecode.enhancement.ondemandload.LazyCollectionWithClosedSessionTestTask;
import org.hibernate.test.bytecode.enhancement.ondemandload.LazyEntityLoadingWithClosedSessionTestTask;
import org.hibernate.test.bytecode.enhancement.pk.EmbeddedPKTestTask;
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
	@TestForIssue( jiraKey = "HHH-9529" )
	public void testFieldHHH9529() {
		EnhancerTestUtils.runEnhancerTestTask( HHH9529TestTask.class );
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

		EnhancerTestUtils.runEnhancerTestTask( LazyBasicPropertyAccessTestTask.class );
		EnhancerTestUtils.runEnhancerTestTask( LazyBasicFieldAccessTestTask.class );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-10252" )
	public void testCascadeDelete() {
		EnhancerTestUtils.runEnhancerTestTask( CascadeDeleteTestTask.class );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-10055" )
	public void testLazyCollectionHandling() {
		EnhancerTestUtils.runEnhancerTestTask( LazyCollectionLoadingTestTask.class );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-10267" )
	public void testLazyGroups() {
		EnhancerTestUtils.runEnhancerTestTask( LazyGroupAccessTestTask.class );
	}

	@Test(timeout = 10000)
	@TestForIssue( jiraKey = "HHH-10055" )
	@FailureExpected( jiraKey = "HHH-10055" )
	public void testOnDemand() {
		EnhancerTestUtils.runEnhancerTestTask( LazyCollectionWithClearedSessionTestTask.class );
		EnhancerTestUtils.runEnhancerTestTask( LazyCollectionWithClosedSessionTestTask.class );
		EnhancerTestUtils.runEnhancerTestTask( LazyEntityLoadingWithClosedSessionTestTask.class );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-10708" )
	public void testLazyUnexpectedDelete() {
		EnhancerTestUtils.runEnhancerTestTask( UnexpectedDeleteOneTestTask.class );
		EnhancerTestUtils.runEnhancerTestTask( UnexpectedDeleteTwoTestTask.class );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-10646" )
	public void testMappedSuperclass() {
		EnhancerTestUtils.runEnhancerTestTask( MappedSuperclassTestTask.class );
	}

	@Test
	public void testMerge() {
		EnhancerTestUtils.runEnhancerTestTask( CompositeMergeTestTask.class );
	}

	@Test
	public void testEmbeddedPK() {
		EnhancerTestUtils.runEnhancerTestTask( EmbeddedPKTestTask.class );
	}

	@Test
	public void testExtendedEnhancement() {
		EnhancerTestUtils.runEnhancerTestTask( ExtendedEnhancementTestTask.class );
		EnhancerTestUtils.runEnhancerTestTask( ExtendedAssociationManagementTestTasK.class );
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

	@Test
	@TestForIssue( jiraKey = "HHH-9937")
	public void testLazyBasicFieldNotInitialized() {
		EnhancerTestUtils.runEnhancerTestTask( LazyBasicFieldNotInitializedTestTask.class );
	}

	@Test
	public void testInitFromCache() {
		EnhancerTestUtils.runEnhancerTestTask( InitFromCacheTestTask.class );
	}
}
