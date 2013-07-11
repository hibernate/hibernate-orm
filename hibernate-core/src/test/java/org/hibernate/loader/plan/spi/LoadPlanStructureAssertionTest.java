/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.loader.plan.spi;

import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.EncapsulatedCompositeIdResultSetProcessorTest;
import org.hibernate.loader.Helper;
import org.hibernate.loader.plan.exec.process.internal.ResultSetProcessorImpl;
import org.hibernate.loader.plan.exec.spi.EntityLoadQueryDetails;
import org.hibernate.loader.plan.exec.spi.RowReader;
import org.hibernate.persister.entity.OuterJoinLoadable;

import org.junit.Test;

import junit.framework.Assert;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.test.annotations.cid.keymanytoone.Card;
import org.hibernate.test.annotations.cid.keymanytoone.CardField;
import org.hibernate.test.annotations.cid.keymanytoone.Key;
import org.hibernate.test.annotations.cid.keymanytoone.PrimaryKey;

import static junit.framework.Assert.assertNotNull;
import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * Used to assert that "fetch graphs" between JoinWalker and LoadPlan are same.
 *
 * @author Steve Ebersole
 */
public class LoadPlanStructureAssertionTest extends BaseUnitTestCase {
	@Test
	public void testJoinedOneToOne() {
		// tests the mappings defined in org.hibernate.test.onetoone.joined.JoinedSubclassOneToOneTest
		Configuration cfg = new Configuration();
		cfg.addResource( "org/hibernate/test/onetoone/joined/Person.hbm.xml" );
		SessionFactoryImplementor sf = (SessionFactoryImplementor) cfg.buildSessionFactory();

//		doCompare( sf, (OuterJoinLoadable) sf.getClassMetadata( org.hibernate.test.onetoone.joined.Person.class ) );
		doCompare( sf, (OuterJoinLoadable) sf.getClassMetadata( org.hibernate.test.onetoone.joined.Entity.class ) );

//		doCompare( sf, (OuterJoinLoadable) sf.getClassMetadata( org.hibernate.test.onetoone.joined.Address.class ) );
	}

	@Test
	public void testSpecialOneToOne() {
		// tests the mappings defined in org.hibernate.test.onetoone.joined.JoinedSubclassOneToOneTest
		Configuration cfg = new Configuration();
		cfg.addResource( "org/hibernate/test/onetoone/formula/Person.hbm.xml" );
		SessionFactoryImplementor sf = (SessionFactoryImplementor) cfg.buildSessionFactory();

		doCompare( sf, (OuterJoinLoadable) sf.getClassMetadata( org.hibernate.test.onetoone.formula.Person.class ) );
	}

	@Test
	public void testEncapsulatedCompositeIdNoFetches() {
		// CardField is an entity with a composite identifier mapped via a @EmbeddedId class (CardFieldPK) defining
		// a @ManyToOne
		//
		// Parent is an entity with a composite identifier mapped via a @EmbeddedId class (ParentPK) which is defined
		// using just basic types (strings, ints, etc)
		Configuration cfg = new Configuration();
		cfg.addAnnotatedClass( EncapsulatedCompositeIdResultSetProcessorTest.Parent.class );
		cfg.addAnnotatedClass( EncapsulatedCompositeIdResultSetProcessorTest.CardField.class );
		cfg.addAnnotatedClass( EncapsulatedCompositeIdResultSetProcessorTest.Card.class );
		SessionFactoryImplementor sf = (SessionFactoryImplementor) cfg.buildSessionFactory();
		doCompare( sf, (OuterJoinLoadable) sf.getClassMetadata( EncapsulatedCompositeIdResultSetProcessorTest.CardField.class ) );
		doCompare( sf, (OuterJoinLoadable) sf.getClassMetadata( EncapsulatedCompositeIdResultSetProcessorTest.Card.class ) );
		doCompare( sf, (OuterJoinLoadable) sf.getClassMetadata( EncapsulatedCompositeIdResultSetProcessorTest.Parent.class ) );
	}

	@Test
	public void testEncapsulatedCompositeIdWithFetches1() {
		Configuration cfg = new Configuration();
		cfg.addAnnotatedClass( Card.class );
		cfg.addAnnotatedClass( CardField.class );
		cfg.addAnnotatedClass( Key.class );
		cfg.addAnnotatedClass( PrimaryKey.class );

		SessionFactoryImplementor sf = (SessionFactoryImplementor) cfg.buildSessionFactory();

		final OuterJoinLoadable cardFieldPersister = (OuterJoinLoadable) sf.getClassMetadata( CardField.class );
		doCompare( sf, cardFieldPersister );
		final LoadPlan loadPlan = LoadPlanStructureAssertionHelper.INSTANCE.buildLoadPlan( sf, cardFieldPersister );
		assertEquals( LoadPlan.Disposition.ENTITY_LOADER, loadPlan.getDisposition() );
		assertEquals( 1, loadPlan.getReturns().size() );
		final EntityReturn cardFieldReturn = assertTyping( EntityReturn.class, loadPlan.getReturns().get( 0 ) );
		assertEquals( 0, cardFieldReturn.getFetches().length );
		assertEquals( 1, cardFieldReturn.getIdentifierDescription().getFetches().length );
		final CompositeFetch cardFieldCompositePkFetch = assertTyping(
				CompositeFetch.class,
				cardFieldReturn.getIdentifierDescription().getFetches()[0]
		);
		assertEquals( 2, cardFieldCompositePkFetch.getFetches().length );
		final EntityFetch cardFetch = assertTyping( EntityFetch.class, cardFieldCompositePkFetch.getFetches()[0] );
		// i think this one might be a mistake; i think the collection reader still needs to be registered.  Its zero
		// because the inverse of the key-many-to-one already had a registered AssociationKey and so saw the
		// CollectionFetch as a circularity (I think)
		assertEquals( 0, cardFetch.getFetches().length );
		assertEquals( 0, cardFetch.getIdentifierDescription().getFetches().length );

		final EntityFetch keyFetch = assertTyping( EntityFetch.class, cardFieldCompositePkFetch.getFetches()[1] );
		assertEquals( 0, keyFetch.getFetches().length );
		assertEquals( 0, keyFetch.getIdentifierDescription().getFetches().length );

		// we need the readers ordered in a certain manner.  Here specifically: Fetch(Card), Fetch(Key), Return(CardField)
		//
		// additionally, we need Fetch(Card) and Fetch(Key) to be hydrated/semi-resolved before attempting to
		// resolve the EntityKey for Return(CardField)
		//
		// together those sound like argument enough to continue keeping readers for "identifier fetches" as part of
		// a special "identifier reader".  generated aliases could help here too to remove cyclic-ness from the graph.
		// but at any rate, we need to know still when this becomes circularity
	}

	@Test
	public void testEncapsulatedCompositeIdWithFetches2() {
		Configuration cfg = new Configuration();
		cfg.addAnnotatedClass( Card.class );
		cfg.addAnnotatedClass( CardField.class );
		cfg.addAnnotatedClass( Key.class );
		cfg.addAnnotatedClass( PrimaryKey.class );

		SessionFactoryImplementor sf = (SessionFactoryImplementor) cfg.buildSessionFactory();

		final OuterJoinLoadable cardPersister = (OuterJoinLoadable) sf.getClassMetadata( Card.class );
		doCompare( sf, cardPersister );
		final LoadPlan cardLoadPlan = LoadPlanStructureAssertionHelper.INSTANCE.buildLoadPlan( sf, cardPersister );
		assertEquals( LoadPlan.Disposition.ENTITY_LOADER, cardLoadPlan.getDisposition() );
		assertEquals( 1, cardLoadPlan.getReturns().size() );
		final EntityReturn cardReturn = assertTyping( EntityReturn.class, cardLoadPlan.getReturns().get( 0 ) );
		assertEquals( 0, cardReturn.getIdentifierDescription().getFetches().length );
		assertEquals( 1, cardReturn.getFetches().length );
		final CollectionFetch fieldsFetch = assertTyping( CollectionFetch.class, cardReturn.getFetches()[0] );
		assertNotNull( fieldsFetch.getElementGraph() );
		final EntityElementGraph fieldsElementElementGraph = assertTyping( EntityElementGraph.class, fieldsFetch.getElementGraph() );
		assertEquals( 0, fieldsElementElementGraph.getFetches().length );
		assertEquals( 1, fieldsElementElementGraph.getIdentifierDescription().getFetches().length );
		final CompositeFetch fieldsElementCompositeIdFetch = assertTyping(
				CompositeFetch.class,
				fieldsElementElementGraph.getIdentifierDescription().getFetches()[0]
		);
		assertEquals( 2, fieldsElementCompositeIdFetch.getFetches().length );

		BidirectionalEntityFetch circularCardFetch = assertTyping(
				BidirectionalEntityFetch.class,
				fieldsElementCompositeIdFetch.getFetches()[0]
		);
		assertSame( circularCardFetch.getTargetEntityReference(), cardReturn );

		// the fetch above is to the other key-many-to-one for CardField.primaryKey composite: key
		EntityFetch keyFetch = assertTyping(
				EntityFetch.class,
				fieldsElementCompositeIdFetch.getFetches()[1]
		);
		assertEquals( Key.class.getName(), keyFetch.getEntityPersister().getEntityName() );


		final EntityLoadQueryDetails queryDetails = Helper.INSTANCE.buildLoadQueryDetails( cardLoadPlan, sf );
		final ResultSetProcessorImpl resultSetProcessor = assertTyping(
				ResultSetProcessorImpl.class,
				queryDetails.getResultSetProcessor()
		);
		final EntityLoadQueryDetails.EntityLoaderRowReader rowReader = assertTyping(
				EntityLoadQueryDetails.EntityLoaderRowReader.class,
				resultSetProcessor.getRowReader()
		);
	}

	@Test
	public void testManyToMany() {
		Configuration cfg = new Configuration();
		cfg.addResource( "org/hibernate/test/immutable/entitywithmutablecollection/inverse/ContractVariation.hbm.xml" );
		SessionFactoryImplementor sf = (SessionFactoryImplementor) cfg.buildSessionFactory();
		doCompare( sf, (OuterJoinLoadable) sf.getClassMetadata( org.hibernate.test.immutable.entitywithmutablecollection.Contract.class ) );

	}

	private void doCompare(SessionFactoryImplementor sf, OuterJoinLoadable persister) {
		LoadPlanStructureAssertionHelper.INSTANCE.performBasicComparison( sf, persister );
	}
}
