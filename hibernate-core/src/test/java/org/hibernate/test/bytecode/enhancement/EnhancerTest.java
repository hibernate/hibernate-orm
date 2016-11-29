/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement;

import org.hibernate.bytecode.enhance.spi.UnloadedClass;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestContext;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.test.bytecode.enhancement.access.MixedAccessTestTask;
import org.hibernate.test.bytecode.enhancement.association.InheritedAttributeAssociationTestTask;
import org.hibernate.test.bytecode.enhancement.association.ManyToManyAssociationTestTask;
import org.hibernate.test.bytecode.enhancement.association.OneToManyAssociationTestTask;
import org.hibernate.test.bytecode.enhancement.association.OneToOneAssociationTestTask;
import org.hibernate.test.bytecode.enhancement.basic.BasicEnhancementTestTask;
import org.hibernate.test.bytecode.enhancement.basic.HHH9529TestTask;
import org.hibernate.test.bytecode.enhancement.cascade.CascadeDeleteTestTask;
import org.hibernate.test.bytecode.enhancement.dirty.DirtyTrackingCollectionTestTask;
import org.hibernate.test.bytecode.enhancement.dirty.DirtyTrackingTestTask;
import org.hibernate.test.bytecode.enhancement.eviction.EvictionTestTask;
import org.hibernate.test.bytecode.enhancement.extended.ExtendedAssociationManagementTestTasK;
import org.hibernate.test.bytecode.enhancement.extended.ExtendedEnhancementTestTask;
import org.hibernate.test.bytecode.enhancement.inherited.InheritedTestTask;
import org.hibernate.test.bytecode.enhancement.join.HHH3949TestTask1;
import org.hibernate.test.bytecode.enhancement.join.HHH3949TestTask2;
import org.hibernate.test.bytecode.enhancement.join.HHH3949TestTask3;
import org.hibernate.test.bytecode.enhancement.join.HHH3949TestTask4;
import org.hibernate.test.bytecode.enhancement.lazy.HHH_10708.UnexpectedDeleteOneTestTask;
import org.hibernate.test.bytecode.enhancement.lazy.HHH_10708.UnexpectedDeleteThreeTestTask;
import org.hibernate.test.bytecode.enhancement.lazy.HHH_10708.UnexpectedDeleteTwoTestTask;
import org.hibernate.test.bytecode.enhancement.lazy.LazyBasicFieldNotInitializedTestTask;
import org.hibernate.test.bytecode.enhancement.lazy.LazyCollectionLoadingTestTask;
import org.hibernate.test.bytecode.enhancement.lazy.LazyCollectionNoTransactionLoadingTestTask;
import org.hibernate.test.bytecode.enhancement.lazy.LazyLoadingIntegrationTestTask;
import org.hibernate.test.bytecode.enhancement.lazy.LazyLoadingTestTask;
import org.hibernate.test.bytecode.enhancement.lazy.LazyProxyOnEnhancedEntityTestTask;
import org.hibernate.test.bytecode.enhancement.lazy.basic.LazyBasicFieldAccessTestTask;
import org.hibernate.test.bytecode.enhancement.lazy.basic.LazyBasicPropertyAccessTestTask;
import org.hibernate.test.bytecode.enhancement.lazy.cache.LazyInCacheTestTask;
import org.hibernate.test.bytecode.enhancement.lazy.group.LazyGroupAccessTestTask;
import org.hibernate.test.bytecode.enhancement.lazy.group.LazyGroupUpdateTestTask;
import org.hibernate.test.bytecode.enhancement.lazy.group.SimpleLazyGroupUpdateTestTask;
import org.hibernate.test.bytecode.enhancement.lazyCache.InitFromCacheTestTask;
import org.hibernate.test.bytecode.enhancement.mapped.MappedSuperclassTestTask;
import org.hibernate.test.bytecode.enhancement.merge.CompositeMergeTestTask;
import org.hibernate.test.bytecode.enhancement.ondemandload.LazyCollectionWithClearedSessionTestTask;
import org.hibernate.test.bytecode.enhancement.ondemandload.LazyCollectionWithClosedSessionTestTask;
import org.hibernate.test.bytecode.enhancement.ondemandload.LazyEntityLoadingWithClosedSessionTestTask;
import org.hibernate.test.bytecode.enhancement.otherentityentrycontext.OtherEntityEntryContextTestTask;
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
	@TestForIssue( jiraKey = "HHH-10851" )
	public void testAccess() {
		EnhancerTestUtils.runEnhancerTestTask( MixedAccessTestTask.class );
	}

	@Test
	public void testDirty() {
		EnhancerTestUtils.runEnhancerTestTask( DirtyTrackingTestTask.class );
	}

	@Test
	public void testEviction() {
		EnhancerTestUtils.runEnhancerTestTask( EvictionTestTask.class );
	}

	@Test
	public void testOtherPersistenceContext() {
		EnhancerTestUtils.runEnhancerTestTask( OtherEntityEntryContextTestTask.class );
	}

	@Test
	public void testAssociation() {
		EnhancerTestUtils.runEnhancerTestTask( OneToOneAssociationTestTask.class );
		EnhancerTestUtils.runEnhancerTestTask( OneToManyAssociationTestTask.class );
		EnhancerTestUtils.runEnhancerTestTask( ManyToManyAssociationTestTask.class );
		EnhancerTestUtils.runEnhancerTestTask( InheritedAttributeAssociationTestTask.class );
	}

	@Test
	public void testLazy() {
		EnhancerTestUtils.runEnhancerTestTask( LazyLoadingTestTask.class );
		EnhancerTestUtils.runEnhancerTestTask( LazyLoadingIntegrationTestTask.class );

		EnhancerTestUtils.runEnhancerTestTask( LazyBasicPropertyAccessTestTask.class );
		EnhancerTestUtils.runEnhancerTestTask( LazyBasicFieldAccessTestTask.class );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-10922" )
	public void testLazyProxyOnEnhancedEntity() {
		EnhancerTestUtils.runEnhancerTestTask( LazyProxyOnEnhancedEntityTestTask.class, new EnhancerTestContext() {
			@Override
			public boolean hasLazyLoadableAttributes(UnloadedClass classDescriptor) {
				return false;
			}
		} );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11173" )
	public void testLazyCache() {
		EnhancerTestUtils.runEnhancerTestTask( LazyInCacheTestTask.class );
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

	@Test
	@TestForIssue( jiraKey = "HHH-11155" )
	public void testLazyGroupsUpdate() {
		EnhancerTestUtils.runEnhancerTestTask( LazyGroupUpdateTestTask.class );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11155" )
	public void testLazyGroupsUpdateSimple() {
		EnhancerTestUtils.runEnhancerTestTask( SimpleLazyGroupUpdateTestTask.class );
	}

	@Test
	public void testLazyCollectionNoTransactionHandling() {
		EnhancerTestUtils.runEnhancerTestTask( LazyCollectionNoTransactionLoadingTestTask.class );
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
		EnhancerTestUtils.runEnhancerTestTask( UnexpectedDeleteThreeTestTask.class );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-10646" )
	public void testMappedSuperclass() {
		EnhancerTestUtils.runEnhancerTestTask( MappedSuperclassTestTask.class );
		EnhancerTestUtils.runEnhancerTestTask( MappedSuperclassTestTask.class, new EnhancerTestContext() {
			@Override
			public boolean hasLazyLoadableAttributes(UnloadedClass classDescriptor) {
				// HHH-10981 - Without lazy loading, the generation of getters and setters has a different code path
				return false;
			}
		} );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11284" )
	public void testInherited() {
		EnhancerTestUtils.runEnhancerTestTask( InheritedTestTask.class );
		EnhancerTestUtils.runEnhancerTestTask( InheritedTestTask.class, new EnhancerTestContext() {
			@Override
			public boolean hasLazyLoadableAttributes(UnloadedClass classDescriptor) {
				// HHH-10981 - Without lazy loading, the generation of getters and setters has a different code path
				return false;
			}
		} );
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
	@TestForIssue( jiraKey = "HHH-11293")
	public void testDirtyCollection() {
		EnhancerTestUtils.runEnhancerTestTask( DirtyTrackingCollectionTestTask.class );
	}

	@Test
	public void testInitFromCache() {
		EnhancerTestUtils.runEnhancerTestTask( InitFromCacheTestTask.class );
	}
}
