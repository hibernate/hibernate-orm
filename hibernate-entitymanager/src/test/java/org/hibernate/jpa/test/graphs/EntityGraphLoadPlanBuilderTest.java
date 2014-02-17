/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc..
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
 *
 */
package org.hibernate.jpa.test.graphs;

import java.util.Iterator;
import java.util.Set;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;


import org.junit.Test;

import static org.junit.Assert.*;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.loader.plan.build.internal.FetchGraphLoadPlanBuildingStrategy;
import org.hibernate.loader.plan.build.internal.LoadGraphLoadPlanBuildingStrategy;
import org.hibernate.loader.plan.build.internal.AbstractLoadPlanBuildingAssociationVisitationStrategy;
import org.hibernate.loader.plan.build.spi.LoadPlanTreePrinter;
import org.hibernate.loader.plan.build.spi.MetamodelDrivenLoadPlanBuilder;
import org.hibernate.loader.plan.exec.internal.AliasResolutionContextImpl;
import org.hibernate.loader.plan.spi.Join;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.loader.plan.spi.QuerySpace;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class EntityGraphLoadPlanBuilderTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Cat.class, Person.class, Country.class, Dog.class, ExpressCompany.class, Address.class };
	}

	@Entity
	public static class Dog {
		@Id
		String name;
		@ElementCollection
		Set<String> favorites;
	}

	@Entity
	public static class Cat {
		@Id
		String name;
		@ManyToOne(fetch = FetchType.LAZY)
		Person owner;

	}

	@Entity
	public static class Person {
		@Id
		String name;
		@OneToMany(mappedBy = "owner")
		Set<Cat> pets;
		@Embedded
		Address homeAddress;
	}

	@Embeddable
	public static class Address {
		@ManyToOne
		Country country;
	}

	@Entity
	public static class ExpressCompany {
		@Id
		String name;
		@ElementCollection
		Set<Address> shipAddresses;
	}

	@Entity
	public static class Country {
		@Id
		String name;
	}

	/**
	 * EntityGraph(1):
	 *
	 * Cat
	 *
	 * LoadPlan:
	 *
	 * Cat
	 *
	 * ---------------------
	 *
	 * EntityGraph(2):
	 *
	 * Cat
	 * owner -- Person
	 *
	 * LoadPlan:
	 *
	 * Cat
	 * owner -- Person
	 * address --- Address
	 */
	@Test
	public void testBasicFetchLoadPlanBuilding() {
		EntityManager em = getOrCreateEntityManager();
		EntityGraph eg = em.createEntityGraph( Cat.class );
		LoadPlan plan = buildLoadPlan( eg, Mode.FETCH, Cat.class );
		LoadPlanTreePrinter.INSTANCE.logTree( plan, new AliasResolutionContextImpl( sfi() ) );
		QuerySpace rootQuerySpace = plan.getQuerySpaces().getRootQuerySpaces().get( 0 );
		assertFalse(
				"With fetchgraph property and an empty EntityGraph, there should be no join at all",
				rootQuerySpace.getJoins().iterator().hasNext()
		);
		// -------------------------------------------------- another a little more complicated case
		eg = em.createEntityGraph( Cat.class );
		eg.addSubgraph( "owner", Person.class );
		plan = buildLoadPlan( eg, Mode.FETCH, Cat.class );
		LoadPlanTreePrinter.INSTANCE.logTree( plan, new AliasResolutionContextImpl( sfi() ) );
		rootQuerySpace = plan.getQuerySpaces().getRootQuerySpaces().get( 0 );
		Iterator<Join> iterator = rootQuerySpace.getJoins().iterator();
		assertTrue(
				"With fetchgraph property and an empty EntityGraph, there should be no join at all", iterator.hasNext()
		);
		Join personJoin = iterator.next();
		assertNotNull( personJoin );
		QuerySpace.Disposition disposition = personJoin.getRightHandSide().getDisposition();
		assertEquals(
				"This should be an entity join which fetches Person", QuerySpace.Disposition.ENTITY, disposition
		);

		iterator = personJoin.getRightHandSide().getJoins().iterator();
		assertTrue( "The composite address should be fetched", iterator.hasNext() );
		Join addressJoin = iterator.next();
		assertNotNull( addressJoin );
		disposition = addressJoin.getRightHandSide().getDisposition();
		assertEquals( QuerySpace.Disposition.COMPOSITE, disposition );
		assertFalse( iterator.hasNext() );
		assertFalse(
				"The ManyToOne attribute in composite should not be fetched",
				addressJoin.getRightHandSide().getJoins().iterator().hasNext()
		);
		em.close();
	}

	/**
	 * EntityGraph(1):
	 *
	 * Cat
	 *
	 * LoadPlan:
	 *
	 * Cat
	 *
	 * ---------------------
	 *
	 * EntityGraph(2):
	 *
	 * Cat
	 * owner -- Person
	 *
	 * LoadPlan:
	 *
	 * Cat
	 * owner -- Person
	 * address --- Address
	 * country -- Country
	 */
	@Test
	public void testBasicLoadLoadPlanBuilding() {
		EntityManager em = getOrCreateEntityManager();
		EntityGraph eg = em.createEntityGraph( Cat.class );
		LoadPlan plan = buildLoadPlan( eg, Mode.LOAD, Cat.class );
		LoadPlanTreePrinter.INSTANCE.logTree( plan, new AliasResolutionContextImpl( sfi() ) );
		QuerySpace rootQuerySpace = plan.getQuerySpaces().getRootQuerySpaces().get( 0 );
		assertFalse(
				"With fetchgraph property and an empty EntityGraph, there should be no join at all",
				rootQuerySpace.getJoins().iterator().hasNext()
		);
		// -------------------------------------------------- another a little more complicated case
		eg = em.createEntityGraph( Cat.class );
		eg.addSubgraph( "owner", Person.class );
		plan = buildLoadPlan( eg, Mode.LOAD, Cat.class );
		LoadPlanTreePrinter.INSTANCE.logTree( plan, new AliasResolutionContextImpl( sfi() ) );
		rootQuerySpace = plan.getQuerySpaces().getRootQuerySpaces().get( 0 );
		Iterator<Join> iterator = rootQuerySpace.getJoins().iterator();
		assertTrue(
				"With fetchgraph property and an empty EntityGraph, there should be no join at all", iterator.hasNext()
		);
		Join personJoin = iterator.next();
		assertNotNull( personJoin );
		QuerySpace.Disposition disposition = personJoin.getRightHandSide().getDisposition();
		assertEquals(
				"This should be an entity join which fetches Person", QuerySpace.Disposition.ENTITY, disposition
		);

		iterator = personJoin.getRightHandSide().getJoins().iterator();
		assertTrue( "The composite address should be fetched", iterator.hasNext() );
		Join addressJoin = iterator.next();
		assertNotNull( addressJoin );
		disposition = addressJoin.getRightHandSide().getDisposition();
		assertEquals( QuerySpace.Disposition.COMPOSITE, disposition );
		iterator = addressJoin.getRightHandSide().getJoins().iterator();
		assertTrue( iterator.hasNext() );
		Join countryJoin = iterator.next();
		assertNotNull( countryJoin );
		disposition = countryJoin.getRightHandSide().getDisposition();
		assertEquals( QuerySpace.Disposition.ENTITY, disposition );
		assertFalse(
				"The ManyToOne attribute in composite should not be fetched",
				countryJoin.getRightHandSide().getJoins().iterator().hasNext()
		);
		em.close();
	}


	@Test
	public void testBasicElementCollections() {
		EntityManager em = getOrCreateEntityManager();
		EntityGraph eg = em.createEntityGraph( Dog.class );
		eg.addAttributeNodes( "favorites" );
		LoadPlan loadLoadPlan = buildLoadPlan( eg, Mode.LOAD, Dog.class ); //WTF name!!!
		LoadPlanTreePrinter.INSTANCE.logTree( loadLoadPlan, new AliasResolutionContextImpl( sfi() ) );
		QuerySpace querySpace = loadLoadPlan.getQuerySpaces().getRootQuerySpaces().iterator().next();
		Iterator<Join> iterator = querySpace.getJoins().iterator();
		assertTrue( iterator.hasNext() );
		Join collectionJoin = iterator.next();
		assertEquals( QuerySpace.Disposition.COLLECTION, collectionJoin.getRightHandSide().getDisposition() );
		assertFalse( iterator.hasNext() );
		//----------------------------------------------------------------
		LoadPlan fetchLoadPlan = buildLoadPlan( eg, Mode.FETCH, Dog.class );
		LoadPlanTreePrinter.INSTANCE.logTree( fetchLoadPlan, new AliasResolutionContextImpl( sfi() ) );
		querySpace = fetchLoadPlan.getQuerySpaces().getRootQuerySpaces().iterator().next();
		iterator = querySpace.getJoins().iterator();
		assertTrue( iterator.hasNext() );
		collectionJoin = iterator.next();
		assertEquals( QuerySpace.Disposition.COLLECTION, collectionJoin.getRightHandSide().getDisposition() );
		assertFalse( iterator.hasNext() );
		em.close();
	}


	@Test
	public void testEmbeddedCollection() {
		EntityManager em = getOrCreateEntityManager();
		EntityGraph eg = em.createEntityGraph( ExpressCompany.class );
		eg.addAttributeNodes( "shipAddresses" );

		LoadPlan loadLoadPlan = buildLoadPlan( eg, Mode.LOAD, ExpressCompany.class ); //WTF name!!!
		LoadPlanTreePrinter.INSTANCE.logTree( loadLoadPlan, new AliasResolutionContextImpl( sfi() ) );

		QuerySpace querySpace = loadLoadPlan.getQuerySpaces().getRootQuerySpaces().iterator().next();
		Iterator<Join> iterator = querySpace.getJoins().iterator();
		assertTrue( iterator.hasNext() );
		Join collectionJoin = iterator.next();
		assertEquals( QuerySpace.Disposition.COLLECTION, collectionJoin.getRightHandSide().getDisposition() );
		assertFalse( iterator.hasNext() );

		iterator = collectionJoin.getRightHandSide().getJoins().iterator();
		assertTrue( iterator.hasNext() );
		Join collectionElementJoin = iterator.next();
		assertFalse( iterator.hasNext() );
		assertEquals( QuerySpace.Disposition.COMPOSITE, collectionElementJoin.getRightHandSide().getDisposition() );

		iterator = collectionElementJoin.getRightHandSide().getJoins().iterator();
		assertTrue( iterator.hasNext() );
		Join countryJoin = iterator.next();
		assertFalse( iterator.hasNext() );
		assertEquals( QuerySpace.Disposition.ENTITY, countryJoin.getRightHandSide().getDisposition() );

		//----------------------------------------------------------------
		LoadPlan fetchLoadPlan = buildLoadPlan( eg, Mode.FETCH, ExpressCompany.class );
		LoadPlanTreePrinter.INSTANCE.logTree( fetchLoadPlan, new AliasResolutionContextImpl( sfi() ) );


		querySpace = fetchLoadPlan.getQuerySpaces().getRootQuerySpaces().iterator().next();
		iterator = querySpace.getJoins().iterator();
		assertTrue( iterator.hasNext() );
		collectionJoin = iterator.next();
		assertEquals( QuerySpace.Disposition.COLLECTION, collectionJoin.getRightHandSide().getDisposition() );
		assertFalse( iterator.hasNext() );

		iterator = collectionJoin.getRightHandSide().getJoins().iterator();
		assertTrue( iterator.hasNext() );
		collectionElementJoin = iterator.next();
		assertFalse( iterator.hasNext() );
		assertEquals( QuerySpace.Disposition.COMPOSITE, collectionElementJoin.getRightHandSide().getDisposition() );

		iterator = collectionElementJoin.getRightHandSide().getJoins().iterator();
		assertFalse( iterator.hasNext() );
		//----------------------------------------------------------------
		em.close();
	}


	private SessionFactoryImplementor sfi() {
		return entityManagerFactory().unwrap( SessionFactoryImplementor.class );
	}

	private LoadPlan buildLoadPlan(EntityGraph entityGraph, Mode mode, Class clazz) {

		LoadQueryInfluencers loadQueryInfluencers = new LoadQueryInfluencers( sfi() );
		if ( Mode.FETCH == mode ) {
			loadQueryInfluencers.setFetchGraph( entityGraph );
		}
		else {
			loadQueryInfluencers.setLoadGraph( entityGraph );
		}
		EntityPersister ep = (EntityPersister) sfi().getClassMetadata( clazz );
		AbstractLoadPlanBuildingAssociationVisitationStrategy strategy = Mode.FETCH == mode ? new FetchGraphLoadPlanBuildingStrategy(
				sfi(), loadQueryInfluencers, LockMode.NONE
		) : new LoadGraphLoadPlanBuildingStrategy( sfi(), loadQueryInfluencers, LockMode.NONE );
		return MetamodelDrivenLoadPlanBuilder.buildRootEntityLoadPlan( strategy, ep );
	}

	public static enum Mode {FETCH, LOAD}
}
