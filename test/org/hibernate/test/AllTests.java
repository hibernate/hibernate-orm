//$Id$
package org.hibernate.test;

import java.lang.reflect.Constructor;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.hibernate.dialect.Dialect;
import org.hibernate.junit.TestSuiteVisitor;
import org.hibernate.test.abstractembeddedcomponents.cid.AbstractCompositeIdTest;
import org.hibernate.test.abstractembeddedcomponents.propertyref.AbstractComponentPropertyRefTest;
import org.hibernate.test.any.AnyTypeTest;
import org.hibernate.test.array.ArrayTest;
import org.hibernate.test.ast.ASTIteratorTest;
import org.hibernate.test.ast.ASTUtilTest;
import org.hibernate.test.batchfetch.BatchFetchTest;
import org.hibernate.test.bidi.AuctionTest;
import org.hibernate.test.bidi.AuctionTest2;
import org.hibernate.test.cache.CacheSuite;
import org.hibernate.test.cascade.BidirectionalOneToManyCascadeTest;
import org.hibernate.test.cascade.MultiPathCascadeTest;
import org.hibernate.test.cascade.RefreshTest;
import org.hibernate.test.cfg.ListenerTest;
import org.hibernate.test.cid.CompositeIdTest;
import org.hibernate.test.collection.bag.PersistentBagTest;
import org.hibernate.test.collection.idbag.PersistentIdBagTest;
import org.hibernate.test.collection.list.PersistentListTest;
import org.hibernate.test.collection.map.PersistentMapTest;
import org.hibernate.test.collection.original.CollectionTest;
import org.hibernate.test.collection.set.PersistentSetTest;
import org.hibernate.test.component.basic.ComponentTest;
import org.hibernate.test.component.cascading.collection.CascadeToComponentCollectionTest;
import org.hibernate.test.component.cascading.toone.CascadeToComponentAssociationTest;
import org.hibernate.test.compositeelement.CompositeElementTest;
import org.hibernate.test.connections.AggressiveReleaseTest;
import org.hibernate.test.connections.BasicConnectionProviderTest;
import org.hibernate.test.connections.CurrentSessionConnectionTest;
import org.hibernate.test.connections.SuppliedConnectionTest;
import org.hibernate.test.criteria.CriteriaQueryTest;
import org.hibernate.test.cuk.CompositePropertyRefTest;
import org.hibernate.test.cut.CompositeUserTypeTest;
import org.hibernate.test.deletetransient.DeleteTransientEntityTest;
import org.hibernate.test.dialect.functional.cache.SQLFunctionsInterSystemsTest;
import org.hibernate.test.dialect.unit.lockhint.SQLServerLockHintsTest;
import org.hibernate.test.dialect.unit.lockhint.SybaseASE15LockHintsTest;
import org.hibernate.test.dialect.unit.lockhint.SybaseLockHintsTest;
import org.hibernate.test.discriminator.DiscriminatorTest;
import org.hibernate.test.dynamicentity.interceptor.InterceptorDynamicEntityTest;
import org.hibernate.test.dynamicentity.tuplizer.TuplizerDynamicEntityTest;
import org.hibernate.test.dynamicentity.tuplizer2.ImprovedTuplizerDynamicEntityTest;
import org.hibernate.test.ecid.EmbeddedCompositeIdTest;
import org.hibernate.test.entitymode.dom4j.accessors.Dom4jAccessorTest;
import org.hibernate.test.entitymode.dom4j.basic.Dom4jTest;
import org.hibernate.test.entitymode.dom4j.many2one.Dom4jManyToOneTest;
import org.hibernate.test.entitymode.map.basic.DynamicClassTest;
import org.hibernate.test.entitymode.multi.MultiRepresentationTest;
import org.hibernate.test.event.collection.BrokenCollectionEventTest;
import org.hibernate.test.event.collection.association.bidirectional.manytomany.BidirectionalManyToManyBagToSetCollectionEventTest;
import org.hibernate.test.event.collection.association.bidirectional.manytomany.BidirectionalManyToManySetToSetCollectionEventTest;
import org.hibernate.test.event.collection.association.bidirectional.onetomany.BidirectionalOneToManyBagCollectionEventTest;
import org.hibernate.test.event.collection.association.bidirectional.onetomany.BidirectionalOneToManySetCollectionEventTest;
import org.hibernate.test.event.collection.association.unidirectional.manytomany.UnidirectionalManyToManyBagCollectionEventTest;
import org.hibernate.test.event.collection.association.unidirectional.onetomany.UnidirectionalOneToManyBagCollectionEventTest;
import org.hibernate.test.event.collection.association.unidirectional.onetomany.UnidirectionalOneToManySetCollectionEventTest;
import org.hibernate.test.event.collection.values.ValuesBagCollectionEventTest;
import org.hibernate.test.exception.SQLExceptionConversionTest;
import org.hibernate.test.extralazy.ExtraLazyTest;
import org.hibernate.test.filter.DynamicFilterTest;
import org.hibernate.test.formulajoin.FormulaJoinTest;
import org.hibernate.test.generated.PartiallyGeneratedComponentTest;
import org.hibernate.test.generated.TimestampGeneratedValuesWithCachingTest;
import org.hibernate.test.generated.TriggerGeneratedValuesWithCachingTest;
import org.hibernate.test.generated.TriggerGeneratedValuesWithoutCachingTest;
import org.hibernate.test.generatedkeys.identity.IdentityGeneratedKeysTest;
import org.hibernate.test.generatedkeys.select.SelectGeneratorTest;
import org.hibernate.test.generatedkeys.seqidentity.SequenceIdentityTest;
import org.hibernate.test.hql.ASTParserLoadingTest;
import org.hibernate.test.hql.BulkManipulationTest;
import org.hibernate.test.hql.ClassicTranslatorTest;
import org.hibernate.test.hql.CriteriaClassicAggregationReturnTest;
import org.hibernate.test.hql.CriteriaHQLAlignmentTest;
import org.hibernate.test.hql.EJBQLTest;
import org.hibernate.test.hql.HQLTest;
import org.hibernate.test.hql.HqlParserTest;
import org.hibernate.test.hql.ScrollableCollectionFetchingTest;
import org.hibernate.test.hql.WithClauseTest;
import org.hibernate.test.id.MultipleHiLoPerTableGeneratorTest;
import org.hibernate.test.idbag.IdBagTest;
import org.hibernate.test.idclass.IdClassTest;
import org.hibernate.test.idgen.enhanced.OptimizerUnitTest;
import org.hibernate.test.idgen.enhanced.SequenceStyleConfigUnitTest;
import org.hibernate.test.idgen.enhanced.forcedtable.BasicForcedTableSequenceTest;
import org.hibernate.test.idgen.enhanced.forcedtable.HiLoForcedTableSequenceTest;
import org.hibernate.test.idgen.enhanced.forcedtable.PooledForcedTableSequenceTest;
import org.hibernate.test.idgen.enhanced.sequence.BasicSequenceTest;
import org.hibernate.test.idgen.enhanced.sequence.HiLoSequenceTest;
import org.hibernate.test.idgen.enhanced.sequence.PooledSequenceTest;
import org.hibernate.test.idgen.enhanced.table.BasicTableTest;
import org.hibernate.test.idgen.enhanced.table.HiLoTableTest;
import org.hibernate.test.idgen.enhanced.table.PooledTableTest;
import org.hibernate.test.idprops.IdentifierPropertyReferencesTest;
import org.hibernate.test.immutable.ImmutableTest;
import org.hibernate.test.insertordering.InsertOrderingTest;
import org.hibernate.test.instrument.buildtime.InstrumentTest;
import org.hibernate.test.instrument.runtime.CGLIBInstrumentationTest;
import org.hibernate.test.instrument.runtime.JavassistInstrumentationTest;
import org.hibernate.test.interceptor.InterceptorTest;
import org.hibernate.test.interfaceproxy.InterfaceProxyTest;
import org.hibernate.test.iterate.IterateTest;
import org.hibernate.test.join.JoinTest;
import org.hibernate.test.join.OptionalJoinTest;
import org.hibernate.test.joinedsubclass.JoinedSubclassTest;
import org.hibernate.test.joinfetch.JoinFetchTest;
import org.hibernate.test.jpa.cascade.CascadeTest;
import org.hibernate.test.jpa.fetch.FetchingTest;
import org.hibernate.test.jpa.lock.JPALockTest;
import org.hibernate.test.jpa.lock.RepeatableReadTest;
import org.hibernate.test.jpa.proxy.JPAProxyTest;
import org.hibernate.test.jpa.ql.JPAQLComplianceTest;
import org.hibernate.test.jpa.ql.NativeQueryTest;
import org.hibernate.test.jpa.removed.RemovedEntityTest;
import org.hibernate.test.keymanytoone.bidir.component.EagerKeyManyToOneTest;
import org.hibernate.test.keymanytoone.bidir.component.LazyKeyManyToOneTest;
import org.hibernate.test.keymanytoone.bidir.embedded.KeyManyToOneTest;
import org.hibernate.test.lazycache.InstrumentCacheTest;
import org.hibernate.test.lazycache.InstrumentCacheTest2;
import org.hibernate.test.lazyonetoone.LazyOneToOneTest;
import org.hibernate.test.legacy.ABCProxyTest;
import org.hibernate.test.legacy.ABCTest;
import org.hibernate.test.legacy.CacheTest;
import org.hibernate.test.legacy.ComponentNotNullTest;
import org.hibernate.test.legacy.ConfigurationPerformanceTest;
import org.hibernate.test.legacy.FooBarTest;
import org.hibernate.test.legacy.FumTest;
import org.hibernate.test.legacy.IJ2Test;
import org.hibernate.test.legacy.IJTest;
import org.hibernate.test.legacy.MapTest;
import org.hibernate.test.legacy.MasterDetailTest;
import org.hibernate.test.legacy.MultiTableTest;
import org.hibernate.test.legacy.NonReflectiveBinderTest;
import org.hibernate.test.legacy.OneToOneCacheTest;
import org.hibernate.test.legacy.ParentChildTest;
import org.hibernate.test.legacy.QueryByExampleTest;
import org.hibernate.test.legacy.SQLFunctionsTest;
import org.hibernate.test.legacy.SQLLoaderTest;
import org.hibernate.test.legacy.StatisticsTest;
import org.hibernate.test.lob.BlobTest;
import org.hibernate.test.lob.BlobFromLobCreatorDefaultTest;
import org.hibernate.test.lob.BlobFromLobCreatorJDBC3ConnRelOnCloseTest;
import org.hibernate.test.lob.BlobFromLobCreatorJDBC3Test;
import org.hibernate.test.lob.BlobFromLobCreatorJDBC4ConnRelOnCloseTest;
import org.hibernate.test.lob.BlobFromLobCreatorJDBC4Test;
import org.hibernate.test.lob.ClobTest;
import org.hibernate.test.lob.ClobFromLobCreatorDefaultTest;
import org.hibernate.test.lob.ClobFromLobCreatorJDBC3ConnRelOnCloseTest;
import org.hibernate.test.lob.ClobFromLobCreatorJDBC3Test;
import org.hibernate.test.lob.ClobFromLobCreatorJDBC4ConnRelOnCloseTest;
import org.hibernate.test.lob.ClobFromLobCreatorJDBC4Test;
import org.hibernate.test.lob.SerializableTypeTest;
import org.hibernate.test.manytomany.ManyToManyTest;
import org.hibernate.test.manytomanyassociationclass.compositeid.ManyToManyAssociationClassCompositeIdTest;
import org.hibernate.test.manytomanyassociationclass.surrogateid.assigned.ManyToManyAssociationClassAssignedIdTest;
import org.hibernate.test.manytomanyassociationclass.surrogateid.generated.ManyToManyAssociationClassGeneratedIdTest;
import org.hibernate.test.map.MapIndexFormulaTest;
import org.hibernate.test.mapcompelem.MapCompositeElementTest;
import org.hibernate.test.mapelemformula.MapElementFormulaTest;
import org.hibernate.test.mapping.PersistentClassVisitorTest;
import org.hibernate.test.mapping.ValueVisitorTest;
import org.hibernate.test.mappingexception.MappingExceptionTest;
import org.hibernate.test.mixed.MixedTest;
import org.hibernate.test.naturalid.immutable.ImmutableNaturalIdTest;
import org.hibernate.test.naturalid.mutable.MutableNaturalIdTest;
import org.hibernate.test.ondelete.OnDeleteTest;
import org.hibernate.test.onetomany.OneToManyTest;
import org.hibernate.test.onetoone.formula.OneToOneFormulaTest;
import org.hibernate.test.onetoone.joined.JoinedSubclassOneToOneTest;
import org.hibernate.test.onetoone.link.OneToOneLinkTest;
import org.hibernate.test.onetoone.nopojo.DynamicMapOneToOneTest;
import org.hibernate.test.onetoone.optional.OptionalOneToOneTest;
import org.hibernate.test.onetoone.singletable.DiscrimSubclassOneToOneTest;
import org.hibernate.test.ops.CreateTest;
import org.hibernate.test.ops.DeleteTest;
import org.hibernate.test.ops.GetLoadTest;
import org.hibernate.test.ops.MergeTest;
import org.hibernate.test.ops.SaveOrUpdateTest;
import org.hibernate.test.optlock.OptimisticLockTest;
import org.hibernate.test.ordered.OrderByTest;
import org.hibernate.test.orphan.OrphanTest;
import org.hibernate.test.pagination.PaginationTest;
import org.hibernate.test.propertyref.component.complete.CompleteComponentPropertyRefTest;
import org.hibernate.test.propertyref.component.partial.PartialComponentPropertyRefTest;
import org.hibernate.test.propertyref.inheritence.discrim.SubclassPropertyRefTest;
import org.hibernate.test.propertyref.inheritence.joined.JoinedSubclassPropertyRefTest;
import org.hibernate.test.propertyref.inheritence.union.UnionSubclassPropertyRefTest;
import org.hibernate.test.proxy.ProxyTest;
import org.hibernate.test.querycache.QueryCacheTest;
import org.hibernate.test.readonly.ReadOnlyTest;
import org.hibernate.test.reattachment.CollectionReattachmentTest;
import org.hibernate.test.reattachment.ProxyReattachmentTest;
import org.hibernate.test.rowid.RowIdTest;
import org.hibernate.test.sorted.SortTest;
import org.hibernate.test.sql.check.OracleCheckStyleTest;
import org.hibernate.test.sql.hand.custom.datadirect.oracle.DataDirectOracleCustomSQLTest;
import org.hibernate.test.sql.hand.custom.db2.DB2CustomSQLTest;
import org.hibernate.test.sql.hand.custom.mysql.MySQLCustomSQLTest;
import org.hibernate.test.sql.hand.custom.oracle.OracleCustomSQLTest;
import org.hibernate.test.sql.hand.custom.sybase.SybaseCustomSQLTest;
import org.hibernate.test.sql.hand.identity.CustomInsertSQLWithIdentityColumnTest;
import org.hibernate.test.sql.hand.query.NativeSQLQueriesTest;
import org.hibernate.test.stats.SessionStatsTest;
import org.hibernate.test.stats.StatsTest;
import org.hibernate.test.subclassfilter.DiscrimSubclassFilterTest;
import org.hibernate.test.subclassfilter.JoinedSubclassFilterTest;
import org.hibernate.test.subclassfilter.UnionSubclassFilterTest;
import org.hibernate.test.subselect.SubselectTest;
import org.hibernate.test.subselectfetch.SubselectFetchTest;
import org.hibernate.test.ternary.TernaryTest;
import org.hibernate.test.timestamp.TimestampTest;
import org.hibernate.test.tm.CMTTest;
import org.hibernate.test.typedmanytoone.TypedManyToOneTest;
import org.hibernate.test.typedonetoone.TypedOneToOneTest;
import org.hibernate.test.typeparameters.TypeParameterTest;
import org.hibernate.test.unconstrained.UnconstrainedTest;
import org.hibernate.test.unidir.BackrefTest;
import org.hibernate.test.unionsubclass.UnionSubclassTest;
import org.hibernate.test.usercollection.basic.UserCollectionTypeTest;
import org.hibernate.test.usercollection.parameterized.ParameterizedUserCollectionTypeTest;
import org.hibernate.test.util.PropertiesHelperTest;
import org.hibernate.test.util.StringHelperTest;
import org.hibernate.test.util.dtd.EntityResolverTest;
import org.hibernate.test.version.VersionTest;
import org.hibernate.test.version.db.DbVersionTest;
import org.hibernate.test.version.sybase.SybaseTimestampVersioningTest;
import org.hibernate.test.where.WhereTest;

/**
 * @author Gavin King
 */
public class AllTests {

	/**
	 * Returns the entire test suite (both legacy and new
	 *
	 * @return the entire test suite
	 */
	public static Test suite() {
		TestSuite suite = new TestSuite();
		suite.addTest( NewTests.suite() );
		suite.addTest( LegacyTests.suite() );
		return suite;
	}

	/**
	 * Returns the entire test suite (both legacy and new) w/o filtering
	 *
	 * @return the entire test suite
	 */
	public static Test unfilteredSuite() {
		TestSuite suite = new TestSuite();
		suite.addTest( NewTests.unfilteredSuite() );
		suite.addTest( LegacyTests.unfilteredSuite() );
		return suite;
	}

	/**
	 * Runs the entire test suite.
	 * <p/>
	 * @see #suite
	 * @param args n/a
	 */
	public static void main(String args[]) {
		TestRunner.run( suite() );
	}

	/**
	 * An inner class representing the new test suite.
	 */
	public static class NewTests {

		/**
		 * Returns the new test suite (filtered)
		 *
		 * @return the new test suite
		 */
		public static Test suite() {
			return filter( ( TestSuite ) unfilteredSuite() );
		}


		/**
		 * Returns the new test suite (unfiltered)
		 *
		 * @return the new test suite
		 */
		public static Test unfilteredSuite() {
			TestSuite suite = new TestSuite("New tests suite");
			suite.addTest( CreateTest.suite() );
			suite.addTest( DeleteTest.suite() );
			suite.addTest( GetLoadTest.suite() );
			suite.addTest( MergeTest.suite() );
			suite.addTest( SaveOrUpdateTest.suite() );
			suite.addTest( MutableNaturalIdTest.suite() );
			suite.addTest( ImmutableNaturalIdTest.suite() );
			suite.addTest( ComponentTest.suite() );
			suite.addTest( CascadeToComponentCollectionTest.suite() );
			suite.addTest( CascadeToComponentAssociationTest.suite() );
			suite.addTest( ProxyTest.suite() );
			suite.addTest( VersionTest.suite() );
			suite.addTest( TimestampTest.suite() );
			suite.addTest( InterceptorTest.suite() );
			suite.addTest( EmbeddedCompositeIdTest.suite() );
			suite.addTest( ImmutableTest.suite() );
			suite.addTest( ReadOnlyTest.suite() );
			suite.addTest( IdClassTest.suite() );
			suite.addTest( ArrayTest.suite() );
			suite.addTest( TernaryTest.suite() );
			suite.addTest( PersistentBagTest.suite() );
			suite.addTest( PersistentIdBagTest.suite() );
			suite.addTest( PersistentListTest.suite() );
			suite.addTest( PersistentMapTest.suite() );
			suite.addTest( CollectionTest.suite() );
			suite.addTest( PersistentSetTest.suite() );
			suite.addTest( IdBagTest.suite() );
			suite.addTest( MapCompositeElementTest.suite() );
			suite.addTest( MapIndexFormulaTest.suite() );
			suite.addTest( MapElementFormulaTest.suite() );
			suite.addTest( BackrefTest.suite() );
			suite.addTest( BatchFetchTest.suite() );
			suite.addTest( CompositeIdTest.suite() );
			suite.addTest( CompositeElementTest.suite() );
			suite.addTest( CompositePropertyRefTest.suite() );
			suite.addTest( FormulaJoinTest.suite() );
			suite.addTest( DiscriminatorTest.suite() );
			suite.addTest( MultiRepresentationTest.suite() );
			suite.addTest( Dom4jAccessorTest.suite() );
			suite.addTest( Dom4jTest.suite() );
			suite.addTest( Dom4jManyToOneTest.suite() );
			suite.addTest( DynamicClassTest.suite() );
			suite.addTest( DynamicFilterTest.suite() );
			suite.addTest( InterfaceProxyTest.suite() );
			suite.addTest( OrphanTest.suite() );
			suite.addTest( org.hibernate.test.orphan.PropertyRefTest.suite() );
			suite.addTest( JoinTest.suite() );
			suite.addTest( OptionalJoinTest.suite() );
			suite.addTest( JoinedSubclassTest.suite() );
			suite.addTest( org.hibernate.test.unionsubclass2.UnionSubclassTest.suite() );
			suite.addTest( MixedTest.suite() );
			suite.addTest( OneToManyTest.suite() );
			suite.addTest( ManyToManyTest.suite() );
			suite.addTest( ManyToManyAssociationClassCompositeIdTest.suite() );
			suite.addTest( ManyToManyAssociationClassAssignedIdTest.suite() );
			suite.addTest( ManyToManyAssociationClassGeneratedIdTest.suite() );
			suite.addTest( OneToOneFormulaTest.suite() );
			suite.addTest( JoinedSubclassOneToOneTest.suite() );
			suite.addTest( OneToOneLinkTest.suite() );
			suite.addTest( DynamicMapOneToOneTest.suite() );
			suite.addTest( OptionalOneToOneTest.suite() );
			suite.addTest( DiscrimSubclassOneToOneTest.suite() );
			suite.addTest( OptimisticLockTest.suite() );
			suite.addTest( org.hibernate.test.propertyref.basic.PropertyRefTest.suite() );
			suite.addTest( CompleteComponentPropertyRefTest.suite() );
			suite.addTest( PartialComponentPropertyRefTest.suite() );
			suite.addTest( SubclassPropertyRefTest.suite() );
			suite.addTest( JoinedSubclassPropertyRefTest.suite() );
			suite.addTest( UnionSubclassPropertyRefTest.suite() );
			suite.addTest( DB2CustomSQLTest.suite() );
			suite.addTest( DataDirectOracleCustomSQLTest.suite() );
			suite.addTest( OracleCustomSQLTest.suite() );
			suite.addTest( SybaseCustomSQLTest.suite() );
			suite.addTest( MySQLCustomSQLTest.suite() );
			suite.addTest( NativeSQLQueriesTest.suite() );
			suite.addTest( OracleCheckStyleTest.suite() );
			suite.addTest( CustomInsertSQLWithIdentityColumnTest.suite() );
			suite.addTest( CriteriaQueryTest.suite() );
			suite.addTest( SubselectTest.suite() );
			suite.addTest( SubselectFetchTest.suite() );
			suite.addTest( JoinFetchTest.suite() );
			suite.addTest( UnionSubclassTest.suite() );
			suite.addTest( ASTIteratorTest.suite() );
			suite.addTest( HQLTest.suite() );
			suite.addTest( ASTParserLoadingTest.suite() );
			suite.addTest( BulkManipulationTest.suite() );
			suite.addTest( WithClauseTest.suite() );
			suite.addTest( EJBQLTest.suite() );
			suite.addTest( HqlParserTest.suite() );
			suite.addTest( ScrollableCollectionFetchingTest.suite() );
			suite.addTest( ClassicTranslatorTest.suite() );
			suite.addTest( CriteriaHQLAlignmentTest.suite() );
			suite.addTest( CriteriaClassicAggregationReturnTest.suite() );
			suite.addTest( ASTUtilTest.suite() );
			suite.addTest( CacheSuite.suite() );
			suite.addTest( QueryCacheTest.suite() );
			suite.addTest( CompositeUserTypeTest.suite() );
			suite.addTest( TypeParameterTest.suite() );
			suite.addTest( TypedOneToOneTest.suite() );
			suite.addTest( TypedManyToOneTest.suite() );
			suite.addTest( CMTTest.suite() );
			suite.addTest( MultipleHiLoPerTableGeneratorTest.suite() );
			suite.addTest( UnionSubclassFilterTest.suite() );
			suite.addTest( JoinedSubclassFilterTest.suite() );
			suite.addTest( DiscrimSubclassFilterTest.suite() );
			suite.addTest( UnconstrainedTest.suite() );
			suite.addTest( RowIdTest.suite() );
			suite.addTest( OnDeleteTest.suite() );
			suite.addTest( OrderByTest.suite() );
			suite.addTest( SortTest.suite() );
			suite.addTest( WhereTest.suite() );
			suite.addTest( IterateTest.suite() );
			suite.addTest( BidirectionalOneToManyCascadeTest.suite() );
			suite.addTest( RefreshTest.suite() );
			suite.addTest( MultiPathCascadeTest.suite() );
			suite.addTest( ListenerTest.suite() );
			suite.addTest( BrokenCollectionEventTest.suite() );
			suite.addTest( BidirectionalManyToManyBagToSetCollectionEventTest.suite() );
			suite.addTest( BidirectionalManyToManySetToSetCollectionEventTest.suite() );
			suite.addTest( BidirectionalOneToManyBagCollectionEventTest.suite() );
			suite.addTest( BidirectionalOneToManySetCollectionEventTest.suite() );
			suite.addTest( UnidirectionalManyToManyBagCollectionEventTest.suite() );
			suite.addTest( UnidirectionalOneToManyBagCollectionEventTest.suite() );
			suite.addTest( UnidirectionalOneToManySetCollectionEventTest.suite() );
			suite.addTest( ValuesBagCollectionEventTest.suite() );
			suite.addTest( ExtraLazyTest.suite() );
			suite.addTest( StatsTest.suite() );
			suite.addTest( SessionStatsTest.suite() );
			suite.addTest( BasicConnectionProviderTest.suite() );
			suite.addTest( SuppliedConnectionTest.suite() );
			suite.addTest( AggressiveReleaseTest.suite() );
			suite.addTest( CurrentSessionConnectionTest.suite() );
			suite.addTest( SQLExceptionConversionTest.suite() );
			suite.addTest( ValueVisitorTest.suite() );
			suite.addTest( PersistentClassVisitorTest.suite() );
			suite.addTest( AuctionTest.suite() );
			suite.addTest( AuctionTest2.suite() );
			suite.addTest( PaginationTest.suite() );
			suite.addTest( MappingExceptionTest.suite() );
			if ( InstrumentTest.isRunnable() ) {
				suite.addTest( InstrumentTest.suite() );
			}
			if ( LazyOneToOneTest.isRunnable() ) {
				suite.addTest( LazyOneToOneTest.suite() );
			}
			if ( InstrumentCacheTest.isRunnable() ) {
				suite.addTest( InstrumentCacheTest.suite() );
			}
			if ( InstrumentCacheTest2.isRunnable() ) {
				suite.addTest( InstrumentCacheTest2.suite() );
			}
			suite.addTest( CGLIBInstrumentationTest.suite() );
			suite.addTest( JavassistInstrumentationTest.suite() );
			suite.addTest( SybaseTimestampVersioningTest.suite() );
			suite.addTest( DbVersionTest.suite() );
			suite.addTest( TimestampGeneratedValuesWithCachingTest.suite() );
			suite.addTest( TriggerGeneratedValuesWithCachingTest.suite() );
			suite.addTest( TriggerGeneratedValuesWithoutCachingTest.suite() );
			suite.addTest( PartiallyGeneratedComponentTest.suite() );
			suite.addTest( IdentityGeneratedKeysTest.suite() );
			suite.addTest( SelectGeneratorTest.suite() );
			suite.addTest( SequenceIdentityTest.suite() );
			suite.addTest( InterceptorDynamicEntityTest.suite() );
			suite.addTest( TuplizerDynamicEntityTest.suite() );
			suite.addTest( ImprovedTuplizerDynamicEntityTest.suite() );
			suite.addTest( org.hibernate.test.bytecode.cglib.ReflectionOptimizerTest.suite() );
			suite.addTest( org.hibernate.test.bytecode.cglib.InvocationTargetExceptionTest.suite() );
			suite.addTest( org.hibernate.test.bytecode.cglib.CGLIBThreadLocalTest.suite() );
			suite.addTest( org.hibernate.test.bytecode.javassist.ReflectionOptimizerTest.suite() );
			suite.addTest( org.hibernate.test.bytecode.javassist.InvocationTargetExceptionTest.suite() );
			suite.addTest( CascadeTest.suite() );
			suite.addTest( FetchingTest.suite() );
			suite.addTest( JPALockTest.suite() );
			suite.addTest( RepeatableReadTest.suite() );
			suite.addTest( JPAProxyTest.suite() );
			suite.addTest( JPAQLComplianceTest.suite()  );
			suite.addTest( NativeQueryTest.suite() );
			suite.addTest( RemovedEntityTest.suite() );
			suite.addTest( AbstractComponentPropertyRefTest.suite() );
			suite.addTest( AbstractCompositeIdTest.suite() );
			suite.addTest( PropertiesHelperTest.suite() );
			suite.addTest( EntityResolverTest.suite() );
			suite.addTest( StringHelperTest.suite() );
			suite.addTest( AnyTypeTest.suite() );
			suite.addTest( SerializableTypeTest.suite() );
			suite.addTest( BlobTest.suite() );
			suite.addTest( BlobFromLobCreatorDefaultTest.suite() );
			suite.addTest( BlobFromLobCreatorJDBC3ConnRelOnCloseTest.suite() );
			suite.addTest( BlobFromLobCreatorJDBC3Test.suite() );
			suite.addTest( BlobFromLobCreatorJDBC4ConnRelOnCloseTest.suite() );
			suite.addTest( BlobFromLobCreatorJDBC4Test.suite() );
			suite.addTest( ClobTest.suite() );
			suite.addTest( ClobFromLobCreatorDefaultTest.suite() );
			suite.addTest( ClobFromLobCreatorJDBC3ConnRelOnCloseTest.suite() );
			suite.addTest( ClobFromLobCreatorJDBC3Test.suite() );
			suite.addTest( ClobFromLobCreatorJDBC4ConnRelOnCloseTest.suite() );
			suite.addTest( ClobFromLobCreatorJDBC4Test.suite() );
			suite.addTest( IdentifierPropertyReferencesTest.suite() );
			suite.addTest( DeleteTransientEntityTest.suite() );
			suite.addTest( UserCollectionTypeTest.suite() );
			suite.addTest( ParameterizedUserCollectionTypeTest.suite() );
			suite.addTest( KeyManyToOneTest.suite() );
			suite.addTest( LazyKeyManyToOneTest.suite() );
			suite.addTest( EagerKeyManyToOneTest.suite() );
			suite.addTest( SQLFunctionsInterSystemsTest.suite() );
			suite.addTest( SybaseASE15LockHintsTest.suite() );
			suite.addTest( SybaseLockHintsTest.suite() );
			suite.addTest( SQLServerLockHintsTest.suite() );
			suite.addTest( InsertOrderingTest.suite() );
			suite.addTest( CollectionReattachmentTest.suite() );
			suite.addTest( ProxyReattachmentTest.suite() );
			suite.addTest( OptimizerUnitTest.suite() );
			suite.addTest( SequenceStyleConfigUnitTest.suite() );
			suite.addTest( BasicForcedTableSequenceTest.suite() );
			suite.addTest( HiLoForcedTableSequenceTest.suite() );
			suite.addTest( PooledForcedTableSequenceTest.suite() );
			suite.addTest( BasicSequenceTest.suite() );
			suite.addTest( HiLoSequenceTest.suite() );
			suite.addTest( PooledSequenceTest.suite() );
			suite.addTest( BasicTableTest.suite() );
			suite.addTest( HiLoTableTest.suite() );
			suite.addTest( PooledTableTest.suite() );

			return suite;
		}

		/**
		 * Runs the new test suite
		 *
		 * @param args n/a
		 */
		public static void main(String[] args) {
			TestRunner.run( suite() );
		}
	}

	/**
	 * An inner class representing the legacy test suite.
	 */
	public static class LegacyTests {

		/**
		 * Returns the legacy test suite
		 *
		 * @return the legacy test suite
		 */
		public static Test suite() {
			return filter( ( TestSuite ) unfilteredSuite() );
		}

		public static Test unfilteredSuite() {
			TestSuite suite = new TestSuite("Legacy tests suite");
			suite.addTest( FumTest.suite() );
			suite.addTest( MasterDetailTest.suite() );
			suite.addTest( ParentChildTest.suite() );
			suite.addTest( ABCTest.suite() );
			suite.addTest( ABCProxyTest.suite() );
			suite.addTest( SQLFunctionsTest.suite() );
			suite.addTest( SQLLoaderTest.suite() );
			suite.addTest( MultiTableTest.suite() );
			suite.addTest( MapTest.suite() );
			suite.addTest( QueryByExampleTest.suite() );
			suite.addTest( ComponentNotNullTest.suite() );
			suite.addTest( IJTest.suite() );
			suite.addTest( IJ2Test.suite() );
			suite.addTest( FooBarTest.suite() );
			suite.addTest( StatisticsTest.suite() );
			suite.addTest( CacheTest.suite() );
			suite.addTest( OneToOneCacheTest.suite() );
			suite.addTest( NonReflectiveBinderTest.suite() );
			suite.addTest( ConfigurationPerformanceTest.suite() ); // Added to ensure we can utilize the recommended performance tips ;)
			return suite;
		}

		/**
		 * Run the legacy test suite
		 *
		 * @param args n/a
		 */
		public static void main(String[] args) {
			TestRunner.run( suite() );
		}
	}

	private static TestSuite filter(TestSuite testSuite) {
		FilterHandler handler = new FilterHandler();
		TestSuiteVisitor visitor = new TestSuiteVisitor( handler );
		visitor.visit( testSuite );
		return handler.getFilteredTestSuite();
	}

	private static class TestSuiteStackEntry {
		public final TestSuite testSuite;
		public final TestSuiteStackEntry parentEntry;

		public TestSuiteStackEntry(TestSuite testSuite, TestSuiteStackEntry parentEntry) {
			this.testSuite = testSuite;
			this.parentEntry = parentEntry;
			if ( parentEntry != null ) {
				parentEntry.testSuite.addTest( testSuite );
			}
		}
	}

	private static class FilterHandler implements TestSuiteVisitor.Handler {
		private TestSuiteStackEntry topStackElement;
		private TestSuiteStackEntry currentStackElement;
		private Dialect dialect = Dialect.getDialect();

		public void handleTestCase(Test test) {
			if ( test instanceof TestCase ) {
				TestCase hibernateTestCase = ( TestCase ) test;
				if ( ! hibernateTestCase.appliesTo( dialect ) ) {
					System.out.println( "skipping test [" + hibernateTestCase.fullTestName() + "] for dialect [" + dialect.getClass().getName() + "]" );
				}
				else {
					currentStackElement.testSuite.addTest( test );
				}
			}
			else {
				currentStackElement.testSuite.addTest( test );
			}
		}

		public void startingTestSuite(TestSuite suite) {
			currentStackElement = new TestSuiteStackEntry( instantiateCopy( suite ), currentStackElement );
			if ( topStackElement == null ) {
				topStackElement = currentStackElement;
			}
		}

		public void completedTestSuite(TestSuite suite) {
			if ( currentStackElement != null ) {
				currentStackElement = currentStackElement.parentEntry;
			}
		}

		public TestSuite getFilteredTestSuite() {
			return topStackElement.testSuite;
		}

		private static final Class[] EXPECTED_CTOR_SIG = new Class[] { String.class };

		private TestSuite instantiateCopy(TestSuite suite) {
			try {
				Class testSuiteClass = suite.getClass();
				Constructor ctor = testSuiteClass.getDeclaredConstructor( EXPECTED_CTOR_SIG );
				ctor.setAccessible( true );
				return ( TestSuite ) ctor.newInstance( new Object[]  { suite.getName() } );
			}
			catch ( Throwable t ) {
				throw new RuntimeException( "Unable to build test suite copy [" + suite + "]", t );
			}
		}
	}
}
