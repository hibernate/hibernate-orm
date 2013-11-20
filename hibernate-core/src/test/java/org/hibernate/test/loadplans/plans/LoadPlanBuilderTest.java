/*
 * jDocBook, processing of DocBook sources
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
package org.hibernate.test.loadplans.plans;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.CascadingActions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.loader.plan.build.internal.CascadeStyleLoadPlanBuildingAssociationVisitationStrategy;
import org.hibernate.loader.plan.build.internal.FetchStyleLoadPlanBuildingAssociationVisitationStrategy;
import org.hibernate.loader.plan.build.spi.LoadPlanTreePrinter;
import org.hibernate.loader.plan.build.spi.MetamodelDrivenLoadPlanBuilder;
import org.hibernate.loader.plan.exec.internal.AliasResolutionContextImpl;
import org.hibernate.loader.plan.spi.CollectionReturn;
import org.hibernate.loader.plan.spi.EntityFetch;
import org.hibernate.loader.plan.spi.EntityReference;
import org.hibernate.loader.plan.spi.EntityReturn;
import org.hibernate.loader.plan.spi.Fetch;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.loader.plan.spi.Return;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.junit4.ExtraAssertions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * @author Steve Ebersole
 */
public class LoadPlanBuilderTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Message.class, Poster.class };
	}

	@Test
	public void testSimpleBuild() {
		EntityPersister ep = (EntityPersister) sessionFactory().getClassMetadata(Message.class);
		FetchStyleLoadPlanBuildingAssociationVisitationStrategy strategy = new FetchStyleLoadPlanBuildingAssociationVisitationStrategy(
				sessionFactory(),
				LoadQueryInfluencers.NONE,
				LockMode.NONE
		);
		LoadPlan plan = MetamodelDrivenLoadPlanBuilder.buildRootEntityLoadPlan( strategy, ep );
		assertFalse( plan.hasAnyScalarReturns() );
		assertEquals( 1, plan.getReturns().size() );
		Return rtn = plan.getReturns().get( 0 );
		EntityReturn entityReturn = ExtraAssertions.assertTyping( EntityReturn.class, rtn );
		assertNotNull( entityReturn.getFetches() );
		assertEquals( 1, entityReturn.getFetches().length );
		Fetch fetch = entityReturn.getFetches()[0];
		EntityFetch entityFetch = ExtraAssertions.assertTyping( EntityFetch.class, fetch );
		assertNotNull( entityFetch.getFetches() );
		assertEquals( 0, entityFetch.getFetches().length );

		LoadPlanTreePrinter.INSTANCE.logTree( plan, new AliasResolutionContextImpl( sessionFactory() ) );
	}

	@Test
	public void testCascadeBasedBuild() {
		EntityPersister ep = (EntityPersister) sessionFactory().getClassMetadata(Message.class);
		CascadeStyleLoadPlanBuildingAssociationVisitationStrategy strategy = new CascadeStyleLoadPlanBuildingAssociationVisitationStrategy(
				CascadingActions.MERGE,
				sessionFactory(),
				LoadQueryInfluencers.NONE,
				LockMode.NONE
		);
		LoadPlan plan = MetamodelDrivenLoadPlanBuilder.buildRootEntityLoadPlan( strategy, ep );
		assertFalse( plan.hasAnyScalarReturns() );
		assertEquals( 1, plan.getReturns().size() );
		Return rtn = plan.getReturns().get( 0 );
		EntityReturn entityReturn = ExtraAssertions.assertTyping( EntityReturn.class, rtn );
		assertNotNull( entityReturn.getFetches() );
		assertEquals( 1, entityReturn.getFetches().length );
		Fetch fetch = entityReturn.getFetches()[0];
		EntityFetch entityFetch = ExtraAssertions.assertTyping( EntityFetch.class, fetch );
		assertNotNull( entityFetch.getFetches() );
		assertEquals( 0, entityFetch.getFetches().length );

		LoadPlanTreePrinter.INSTANCE.logTree( plan, new AliasResolutionContextImpl( sessionFactory() ) );
	}

	@Test
	public void testCollectionInitializerCase() {
		CollectionPersister cp = sessionFactory().getCollectionPersister( Poster.class.getName() + ".messages" );
		FetchStyleLoadPlanBuildingAssociationVisitationStrategy strategy = new FetchStyleLoadPlanBuildingAssociationVisitationStrategy(
				sessionFactory(),
				LoadQueryInfluencers.NONE,
				LockMode.NONE
		);
		LoadPlan plan = MetamodelDrivenLoadPlanBuilder.buildRootCollectionLoadPlan( strategy, cp );
		assertFalse( plan.hasAnyScalarReturns() );
		assertEquals( 1, plan.getReturns().size() );
		Return rtn = plan.getReturns().get( 0 );
		CollectionReturn collectionReturn = ExtraAssertions.assertTyping( CollectionReturn.class, rtn );

		assertNotNull( collectionReturn.getElementGraph() );
		assertNotNull( collectionReturn.getElementGraph().getFetches() );
		// the collection Message elements are fetched, but Message.poster is not fetched
		// (because that collection is owned by that Poster)
		assertEquals( 0, collectionReturn.getElementGraph().getFetches().length );
		EntityReference entityReference = ExtraAssertions.assertTyping( EntityReference.class, collectionReturn.getElementGraph() );
		assertNotNull( entityReference.getFetches() );
		assertEquals( 0, entityReference.getFetches().length );

		LoadPlanTreePrinter.INSTANCE.logTree( plan, new AliasResolutionContextImpl( sessionFactory() ) );
	}

	@Entity( name = "Message" )
	public static class Message {
		@Id
		private Integer mid;
		private String msgTxt;
		@ManyToOne( cascade = CascadeType.MERGE )
		@JoinColumn
		private Poster poster;
	}

	@Entity( name = "Poster" )
	public static class Poster {
		@Id
		private Integer pid;
		private String name;
		@OneToMany(mappedBy = "poster")
		private List<Message> messages;
	}

}
