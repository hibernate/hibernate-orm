/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.loading.graphs;

import java.util.Set;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EntityGraph;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.EffectiveEntityGraph;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.loader.ast.internal.LoaderSelectBuilder;
import org.hibernate.loader.plan.spi.EntityFetch;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.tree.from.FromClause;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.results.graph.entity.internal.EntityFetchDelayedImpl;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.entity.EntityResult;
import org.hibernate.sql.results.graph.Fetch;

import org.hibernate.testing.hamcrest.AssignableMatcher;
import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hibernate.testing.hamcrest.CollectionMatchers.hasSize;
import static org.hibernate.testing.hamcrest.CollectionMatchers.isEmpty;
import static org.junit.Assert.assertThat;

/**
 * @author Strong Liu
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
				EntityGraphLoadPlanBuilderTest.Cat.class,
				EntityGraphLoadPlanBuilderTest.Person.class,
				EntityGraphLoadPlanBuilderTest.Country.class,
				EntityGraphLoadPlanBuilderTest.Dog.class,
				EntityGraphLoadPlanBuilderTest.ExpressCompany.class
		}
)
@SessionFactory
public class EntityGraphLoadPlanBuilderTest {

	/**
	 * EntityGraph:
	 *
	 * Cat
	 *
	 * LoadPlan:
	 *
	 * Cat
	 */
	@Test
	public void testBasicFetchLoadPlanBuilding(SessionFactoryScope scope) {
		scope.inTransaction(
				em -> {
					EntityGraph eg = em.createEntityGraph( Cat.class );

					final SelectStatement sqlAst = buildSqlSelectAst( Cat.class, eg, GraphSemantic.FETCH, scope );

					final FromClause fromClause = sqlAst.getQuerySpec().getFromClause();
					assertThat( fromClause.getRoots().size(), is( 1 ) );
					final TableGroup rootTableGroup = fromClause.getRoots().get( 0 );
					assertThat( rootTableGroup.getTableGroupJoins(), CollectionMatchers.isEmpty() );

					assertThat( sqlAst.getDomainResultDescriptors(), hasSize( 1 ) );
					final DomainResult domainResult = sqlAst.getDomainResultDescriptors().get( 0 );

					assertThat( domainResult, instanceOf( EntityResult.class ) );
					final EntityResult entityResult = (EntityResult) domainResult;

					assertThat(
							domainResult.getResultJavaTypeDescriptor().getJavaType(),
							AssignableMatcher.assignableTo( Cat.class )
					);

					assertThat( entityResult.getFetches(), hasSize( 1 ) );
					assertThat( entityResult.getFetches().get( 0 ), instanceOf( EntityFetchDelayedImpl.class ) );
				}
		);
	}


	/**
	 * EntityGraph:
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
	@FailureExpected( jiraKey = "HHH-13756", reason = "EntityGraph support not yet implemented" )
	public void testFetchLoadPlanBuildingWithSubgraph(SessionFactoryScope scope) {
		scope.inTransaction(
				em -> {
					EntityGraph eg = em.createEntityGraph( Cat.class );
					eg.addSubgraph( "owner", Person.class );

					final SelectStatement sqlAst = buildSqlSelectAst( Cat.class, eg, GraphSemantic.FETCH, scope );

					// Check the from-clause
					final FromClause fromClause = sqlAst.getQuerySpec().getFromClause();
					assertThat( fromClause.getRoots().size(), is( 1 ) );
					final TableGroup rootTableGroup = fromClause.getRoots().get( 0 );
					assertThat( rootTableGroup.getTableGroupJoins(), hasSize( 1 ) );
					final TableGroupJoin ownerJoin = rootTableGroup.getTableGroupJoins().iterator().next();
					assertThat( ownerJoin, notNullValue() );
					assertThat( ownerJoin.getJoinedGroup().getModelPart(), instanceOf( EntityValuedModelPart.class ) );


					// Check the domain-result graph
					assertThat( sqlAst.getDomainResultDescriptors(), hasSize( 1 ) );
					final DomainResult domainResult = sqlAst.getDomainResultDescriptors().get( 0 );
					assertThat( domainResult, instanceOf( EntityResult.class ) );
					final EntityResult catResult = (EntityResult) domainResult;
					assertThat( catResult.getFetches(), hasSize( 1 ) );
					final Fetch fetch = catResult.getFetches().get( 0 );
					assertThat( fetch, instanceOf( EntityFetch.class ) );
					final EntityFetch ownerFetch = (EntityFetch) fetch;
					assertThat( ownerFetch.getFetchedType().getName(), is( "owner" ) );
					assertThat( ownerFetch.getEntityPersister().getEntityName(), is( Person.class.getName() ) );
				}
		);
	}

	/**
	 * EntityGraph:
	 *
	 * Cat
	 *
	 * LoadPlan:
	 *
	 * Cat
	 */
	@Test
	public void testBasicLoadLoadPlanBuilding(SessionFactoryScope scope) {
		scope.inTransaction(
				em -> {
					EntityGraph eg = em.createEntityGraph( Cat.class );

					final SelectStatement sqlAst = buildSqlSelectAst( Cat.class, eg, GraphSemantic.LOAD, scope );

					final FromClause fromClause = sqlAst.getQuerySpec().getFromClause();
					assertThat( fromClause.getRoots(), hasSize( 1 ) );
					final TableGroup rootTableGroup = fromClause.getRoots().get( 0 );
					assertThat( rootTableGroup.getTableGroupJoins(), isEmpty() );

					assertThat( sqlAst.getDomainResultDescriptors(), hasSize( 1 ) );
					final DomainResult domainResult = sqlAst.getDomainResultDescriptors().get( 0 );
					assertThat( domainResult, instanceOf( EntityResult.class ) );
					assertThat( domainResult.getResultJavaTypeDescriptor().getJavaType().getName(), is( Cat.class.getName() ) );
					final EntityResult entityResult = (EntityResult) domainResult;
					assertThat( entityResult.getFetches(), hasSize( 1 ) );
					assertThat( entityResult.getFetches().get( 0 ), instanceOf( EntityFetchDelayedImpl.class ) );
				}
		);
	}

	/**
	 *EntityGraph:
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
	@FailureExpected( jiraKey = "HHH-13756", reason = "EntityGraph support not yet implemented" )
	public void testLoadLoadPlanBuildingWithSubgraph(SessionFactoryScope scope) {
		scope.inTransaction(
				em -> {
					EntityGraph eg = em.createEntityGraph( Cat.class );
					eg.addSubgraph( "owner", Person.class );

					final SelectStatement sqlAst = buildSqlSelectAst( Cat.class, eg, GraphSemantic.LOAD, scope );

					final FromClause fromClause = sqlAst.getQuerySpec().getFromClause();
					assertThat( fromClause.getRoots(), hasSize( 1 ) );
					final TableGroup rootTableGroup = fromClause.getRoots().get( 0 );
					assertThat( rootTableGroup.getTableGroupJoins(), hasSize( 1 ) );

					assertThat( sqlAst.getDomainResultDescriptors(), hasSize( 1 ) );
					final DomainResult domainResult = sqlAst.getDomainResultDescriptors().get( 0 );
					assertThat( domainResult, instanceOf( EntityResult.class ) );
					assertThat( ( (EntityResult) domainResult ).getFetches(), hasSize( 1 ) );
					final Fetch fetch = ( (EntityResult) domainResult ).getFetches().get( 0 );
					assertThat( fetch, instanceOf( EntityFetch.class ) );
					final EntityFetch ownerFetch = (EntityFetch) fetch;
					assertThat( ownerFetch.getFetchedType().getName(), is( "owner" ) );
					assertThat( ownerFetch.getEntityPersister().getEntityName(), is( Person.class.getName() ) );

					// todo (6.0) : check the sub-fetches for Address
				}
		);
	}


	@Test
	@FailureExpected( jiraKey = "HHH-13756", reason = "EntityGraph support not yet implemented" )
	public void testBasicElementCollectionsLoadGraph(SessionFactoryScope scope) {
		scope.inTransaction(
				em -> {
					EntityGraph eg = em.createEntityGraph( Dog.class );
					eg.addAttributeNodes( "favorites" );

					final SelectStatement sqlAst = buildSqlSelectAst( Dog.class, eg, GraphSemantic.LOAD, scope );

					final FromClause fromClause = sqlAst.getQuerySpec().getFromClause();
					assertThat( fromClause.getRoots(), hasSize( 1 ) );
					final TableGroup root = fromClause.getRoots().get( 0 );
					assertThat( root.getTableGroupJoins(), hasSize( 1 ) );
					final TableGroup joinedGroup = root.getTableGroupJoins().iterator().next().getJoinedGroup();
					assertThat( joinedGroup.getModelPart(), instanceOf( PluralAttributeMapping.class ) );
					final PluralAttributeMapping pluralAttributeMapping = (PluralAttributeMapping) joinedGroup.getModelPart();
					assertThat( pluralAttributeMapping.getAttributeName(), is( "favorites" ) );
				}
		);
	}

	@Test
	@FailureExpected( jiraKey = "HHH-13756", reason = "EntityGraph support not yet implemented" )
	public void testBasicElementCollectionsFetchGraph(SessionFactoryScope scope) {
		scope.inTransaction(
				em -> {
					EntityGraph eg = em.createEntityGraph( Dog.class );
					eg.addAttributeNodes( "favorites" );

					final SelectStatement sqlAst = buildSqlSelectAst( Dog.class, eg, GraphSemantic.FETCH, scope );

					final FromClause fromClause = sqlAst.getQuerySpec().getFromClause();
					assertThat( fromClause.getRoots(), hasSize( 1 ) );
					final TableGroup root = fromClause.getRoots().get( 0 );
					assertThat( root.getTableGroupJoins(), hasSize( 1 ) );
					final TableGroup joinedGroup = root.getTableGroupJoins().iterator().next().getJoinedGroup();
					assertThat( joinedGroup.getModelPart(), instanceOf( PluralAttributeMapping.class ) );
					final PluralAttributeMapping pluralAttributeMapping = (PluralAttributeMapping) joinedGroup.getModelPart();
					assertThat( pluralAttributeMapping.getAttributeName(), is( "favorites" ) );
				}
		);
	}


	@Test
	@FailureExpected( jiraKey = "HHH-13756", reason = "EntityGraph support not yet implemented" )
	public void testEmbeddedCollectionLoadSubgraph(SessionFactoryScope scope) {
		scope.inTransaction(
				em -> {
					EntityGraph eg = em.createEntityGraph( ExpressCompany.class );
					eg.addAttributeNodes( "shipAddresses" );

					final SelectStatement sqlAst = buildSqlSelectAst(
							ExpressCompany.class,
							eg, GraphSemantic.LOAD,
							scope
					);

					final FromClause fromClause = sqlAst.getQuerySpec().getFromClause();
					assertThat( fromClause.getRoots(), hasSize( 1 ) );
					final TableGroup root = fromClause.getRoots().get( 0 );
					assertThat( root.getTableGroupJoins(), hasSize( 1 ) );
					final TableGroup joinedGroup = root.getTableGroupJoins().iterator().next().getJoinedGroup();
					assertThat( joinedGroup.getModelPart(), instanceOf( PluralAttributeMapping.class ) );
					final PluralAttributeMapping pluralAttributeMapping = (PluralAttributeMapping) joinedGroup.getModelPart();
					assertThat( pluralAttributeMapping.getAttributeName(), is( "shipAddresses" ) );
					assertThat( joinedGroup.getTableGroupJoins(), isEmpty() );


//		QuerySpace querySpace = loadLoadPlan.getQuerySpaces().getRootQuerySpaces().iterator().next();
//		Iterator<Join> iterator = querySpace.getJoins().iterator();
//		assertTrue( iterator.hasNext() );
//		Join collectionJoin = iterator.next();
//		assertEquals( QuerySpace.Disposition.COLLECTION, collectionJoin.getRightHandSide().getDisposition() );
//		assertFalse( iterator.hasNext() );
//
//		iterator = collectionJoin.getRightHandSide().getJoins().iterator();
//		assertTrue( iterator.hasNext() );
//		Join collectionElementJoin = iterator.next();
//		assertFalse( iterator.hasNext() );
//		assertEquals( QuerySpace.Disposition.COMPOSITE, collectionElementJoin.getRightHandSide().getDisposition() );
//
//		iterator = collectionElementJoin.getRightHandSide().getJoins().iterator();
//		assertTrue( iterator.hasNext() );
//		Join countryJoin = iterator.next();
//		assertFalse( iterator.hasNext() );
//		assertEquals( QuerySpace.Disposition.ENTITY, countryJoin.getRightHandSide().getDisposition() );

				}
		);
	}

	@Test
	@FailureExpected( jiraKey = "HHH-13756", reason = "EntityGraph support not yet implemented" )
	public void testEmbeddedCollectionFetchSubgraph(SessionFactoryScope scope) {
		scope.inTransaction(
				em -> {
					EntityGraph eg = em.createEntityGraph( ExpressCompany.class );
					eg.addAttributeNodes( "shipAddresses" );

					final SelectStatement sqlAst = buildSqlSelectAst(
							ExpressCompany.class,
							eg, GraphSemantic.FETCH,
							scope
					);

					final FromClause fromClause = sqlAst.getQuerySpec().getFromClause();
					assertThat( fromClause.getRoots(), hasSize( 1 ) );
					final TableGroup root = fromClause.getRoots().get( 0 );
					assertThat( root.getTableGroupJoins(), hasSize( 1 ) );
					final TableGroup joinedGroup = root.getTableGroupJoins().iterator().next().getJoinedGroup();
					assertThat( joinedGroup.getModelPart(), instanceOf( PluralAttributeMapping.class ) );
					final PluralAttributeMapping pluralAttributeMapping = (PluralAttributeMapping) joinedGroup.getModelPart();
					assertThat( pluralAttributeMapping.getAttributeName(), is( "shipAddresses" ) );
					assertThat( joinedGroup.getTableGroupJoins(), isEmpty() );
				}
		);
	}

	private SelectStatement buildSqlSelectAst(
			Class entityType,
			EntityGraph entityGraph,
			GraphSemantic mode,
			SessionFactoryScope scope) {
		final EntityPersister entityDescriptor = scope.getSessionFactory().getDomainModel().getEntityDescriptor( entityType );

		final LoadQueryInfluencers loadQueryInfluencers = new LoadQueryInfluencers( scope.getSessionFactory() );
		final EffectiveEntityGraph effectiveEntityGraph = loadQueryInfluencers.getEffectiveEntityGraph();
		effectiveEntityGraph.applyGraph( ( RootGraphImplementor) entityGraph, mode );

		return LoaderSelectBuilder.createSelect(
				entityDescriptor,
				null,
				entityDescriptor.getIdentifierMapping(),
				null,
				1,
				loadQueryInfluencers,
				LockOptions.READ,
				jdbcParameter -> {},
				scope.getSessionFactory()
		);
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
}
