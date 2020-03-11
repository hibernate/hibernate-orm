/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.loading.entitygraph;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
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
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.tree.from.CompositeTableGroup;
import org.hibernate.sql.ast.tree.from.FromClause;
import org.hibernate.sql.ast.tree.from.StandardTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.collection.internal.DelayedCollectionFetch;
import org.hibernate.sql.results.graph.embeddable.internal.EmbeddableFetchImpl;
import org.hibernate.sql.results.graph.entity.EntityFetch;
import org.hibernate.sql.results.graph.entity.EntityResult;
import org.hibernate.sql.results.graph.entity.internal.EntityFetchDelayedImpl;
import org.hibernate.sql.results.graph.entity.internal.EntityFetchJoinedImpl;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hibernate.testing.hamcrest.AssignableMatcher.assignableTo;
import static org.hibernate.testing.hamcrest.CollectionMatchers.hasSize;
import static org.hibernate.testing.hamcrest.CollectionMatchers.isEmpty;
import static org.junit.Assert.assertThat;

/**
 * @author Strong Liu
 * @author Steve Ebersole
 * @author Nathan Xu
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
@TestForIssue( jiraKey = "HHH-13756" )
public class EntityGraphLoadPlanBuilderTest {

	@Test
	void testBasicFetchLoadPlanBuilding(SessionFactoryScope scope) {
		scope.inTransaction(
				em -> {
					final RootGraphImplementor<Cat> eg = em.createEntityGraph( Cat.class );

					final SelectStatement sqlAst = buildSqlSelectAst( Cat.class, eg, GraphSemantic.FETCH, scope );

					// Check the from-clause
					assertEmptyJoinedGroup( sqlAst );

					// Check the domain-result graph
					assertDomainResult( sqlAst, Cat.class, "owner", Person.class,
										entityFetch -> assertThat( entityFetch, instanceOf( EntityFetchDelayedImpl.class ) )
					);
				}
		);
	}

	@Test
	void testFetchLoadPlanBuildingWithSubgraph(SessionFactoryScope scope) {
		scope.inTransaction(
				em -> {
					final RootGraphImplementor<Cat> eg = em.createEntityGraph( Cat.class );
					eg.addSubgraph( "owner", Person.class );

					final SelectStatement sqlAst = buildSqlSelectAst( Cat.class, eg, GraphSemantic.FETCH, scope );

					// Check the from-clause
					assertEntityValuedJoinedGroup( sqlAst, "owner", Person.class, this::assertPersonHomeAddressJoinedGroup );

					// Check the domain-result graph
					assertDomainResult( sqlAst, Cat.class, "owner", Person.class, entityFetch -> {} );
				}
		);
	}

	@Test
	void testFetchLoadPlanBuildingWithDeepSubgraph(SessionFactoryScope scope) {
		scope.inTransaction(
				em -> {
					final RootGraphImplementor<Cat> eg = em.createEntityGraph( Cat.class );
					eg.addSubgraph( "owner", Person.class ).addSubgraph( "company", ExpressCompany.class );

					final SelectStatement sqlAst = buildSqlSelectAst( Cat.class, eg, GraphSemantic.FETCH, scope );

					// Check the from-clause
					assertEntityValuedJoinedGroup( sqlAst, "owner", Person.class, tableGroup -> {
						Set<TableGroupJoin> tableGroupJoins = tableGroup.getTableGroupJoins();
						Map<String, Class<? extends TableGroup>> tableGroupByName = tableGroupJoins.stream()
								.map( TableGroupJoin::getJoinedGroup )
								.collect( Collectors.toMap(
										tg -> tg.getModelPart().getPartName(),
										TableGroup::getClass
								) );
						Map<String, Class<? extends TableGroup> > expectedTableGroupByName = new HashMap<>();
						expectedTableGroupByName.put( "homeAddress", CompositeTableGroup.class );
						expectedTableGroupByName.put( "company", StandardTableGroup.class );
						assertThat( tableGroupByName, is( expectedTableGroupByName ) );
					} );

					// Check the domain-result graph
					assertDomainResult( sqlAst, Cat.class, "owner", Person.class, entityFetch -> {
						assertThat( entityFetch, instanceOf( EntityFetchJoinedImpl.class ) );
						final EntityResult ownerEntityResult = ( (EntityFetchJoinedImpl) entityFetch ).getEntityResult();
						final Map<String, Class<? extends Fetch>> fetchClassByAttributeName = ownerEntityResult.getFetches()
								.stream().collect( Collectors.toMap(
										fetch -> fetch.getFetchedMapping().getPartName(),
										Fetch::getClass
								) );
						final Map<String, Class<? extends Fetch>> expectedFetchClassByAttributeName = new HashMap<>();
						expectedFetchClassByAttributeName.put( "homeAddress", EmbeddableFetchImpl.class );
						expectedFetchClassByAttributeName.put( "pets", DelayedCollectionFetch.class );
						expectedFetchClassByAttributeName.put( "company", EntityFetchJoinedImpl.class );
						assertThat( fetchClassByAttributeName, is( expectedFetchClassByAttributeName ) );

						final Fetch companyFetch = ownerEntityResult.findFetch( "company" );
						assertThat( companyFetch, notNullValue() );

						final EntityResult companyEntityResult = ( (EntityFetchJoinedImpl) companyFetch).getEntityResult();
						assertThat( companyEntityResult.getFetches(), hasSize( 1 ) );

						final Fetch shipAddressesFetch = companyEntityResult.getFetches().get( 0 );
						assertThat( shipAddressesFetch.getFetchedMapping().getPartName(), is( "shipAddresses" ) );
						assertThat( shipAddressesFetch, instanceOf( DelayedCollectionFetch.class ) );
					} );
				}
		);
	}

	@Test
	void testBasicLoadLoadPlanBuilding(SessionFactoryScope scope) {
		scope.inTransaction(
				em -> {
					final RootGraphImplementor<Cat> eg = em.createEntityGraph( Cat.class );

					final SelectStatement sqlAst = buildSqlSelectAst( Cat.class, eg, GraphSemantic.LOAD, scope );

					// Check the from-clause
					assertEmptyJoinedGroup( sqlAst );

					// Check the domain-result graph
					assertDomainResult( sqlAst, Cat.class, "owner", Person.class,
										entityFetch -> assertThat( entityFetch, instanceOf( EntityFetchDelayedImpl.class ) ) );
				}
		);
	}

	@Test
	void testLoadLoadPlanBuildingWithSubgraph(SessionFactoryScope scope) {
		scope.inTransaction(
				em -> {
					final RootGraphImplementor<Cat> eg = em.createEntityGraph( Cat.class );
					eg.addSubgraph( "owner", Person.class );

					final SelectStatement sqlAst = buildSqlSelectAst( Cat.class, eg, GraphSemantic.LOAD, scope );

					// Check the from-clause
					assertEntityValuedJoinedGroup( sqlAst, "owner", Person.class, this::assertPersonHomeAddressJoinedGroup );

					// Check the domain-result graph
					assertDomainResult( sqlAst, Cat.class, "owner", Person.class, entityFetch -> {
						assertThat( entityFetch, instanceOf( EntityFetchJoinedImpl.class ) );
						final EntityResult entityResult = ( (EntityFetchJoinedImpl) entityFetch ).getEntityResult();
						final Map<String, Class<? extends Fetch>> fetchClassByAttributeName = entityResult.getFetches().stream().collect( Collectors.toMap(
								fetch -> fetch.getFetchedMapping().getPartName(),
								Fetch::getClass
						) );
						final Map<String, Class<? extends Fetch>> expectedFetchClassByAttributeName = new HashMap<>();
						expectedFetchClassByAttributeName.put( "pets", DelayedCollectionFetch.class );
						expectedFetchClassByAttributeName.put( "homeAddress", EmbeddableFetchImpl.class );
						expectedFetchClassByAttributeName.put( "company", EntityFetchDelayedImpl.class );
						assertThat( fetchClassByAttributeName, is( expectedFetchClassByAttributeName ) );
					} );
				}
		);
	}

	@Test
	void testBasicElementCollectionsLoadGraph(SessionFactoryScope scope) {
		scope.inTransaction(
				em -> {
					final RootGraphImplementor<Dog> eg = em.createEntityGraph( Dog.class );
					eg.addAttributeNodes( "favorites" );

					final SelectStatement sqlAst = buildSqlSelectAst( Dog.class, eg, GraphSemantic.LOAD, scope );

					// Check the from-clause
					assertPluralAttributeJoinedGroup( sqlAst, "favorites", tableGroup -> {} );
				}
		);
	}

	@Test
	void testBasicElementCollectionsFetchGraph(SessionFactoryScope scope) {
		scope.inTransaction(
				em -> {
					final RootGraphImplementor<Dog> eg = em.createEntityGraph( Dog.class );
					eg.addAttributeNodes( "favorites" );

					final SelectStatement sqlAst = buildSqlSelectAst( Dog.class, eg, GraphSemantic.FETCH, scope );

					// Check the from-clause
					assertPluralAttributeJoinedGroup( sqlAst, "favorites", tableGroup -> {} );
				}
		);
	}

	@Test
	void testEmbeddedCollectionLoadGraph(SessionFactoryScope scope) {
		scope.inTransaction(
				em -> {
					final RootGraphImplementor<ExpressCompany> eg = em.createEntityGraph( ExpressCompany.class );
					eg.addAttributeNodes( "shipAddresses" );

					final SelectStatement sqlAst = buildSqlSelectAst(
							ExpressCompany.class,
							eg, GraphSemantic.LOAD,
							scope
					);

					// Check the from-clause
					assertPluralAttributeJoinedGroup( sqlAst, "shipAddresses", tableGroup -> {
						assertThat( tableGroup.getTableGroupJoins(), hasSize( 1 ) );

						final TableGroup compositeTableGroup = tableGroup.getTableGroupJoins().iterator().next().getJoinedGroup();
						assertThat( compositeTableGroup, instanceOf( CompositeTableGroup.class ) );
						assertThat( compositeTableGroup.getTableGroupJoins(), hasSize( 1 ) );

						final TableGroup countryTableGroup = compositeTableGroup.getTableGroupJoins().iterator().next().getJoinedGroup();
						assertThat( countryTableGroup.getModelPart().getPartName(), is( "country" ) );

						assertThat( countryTableGroup.getTableGroupJoins(), isEmpty() );
					} );

				}
		);
	}

	@Test
	void testEmbeddedCollectionFetchGraph(SessionFactoryScope scope) {
		scope.inTransaction(
				em -> {
					final RootGraphImplementor<ExpressCompany> eg = em.createEntityGraph( ExpressCompany.class );
					eg.addAttributeNodes( "shipAddresses" );

					final SelectStatement sqlAst = buildSqlSelectAst(
							ExpressCompany.class,
							eg, GraphSemantic.FETCH,
							scope
					);

					// Check the from-clause
					assertPluralAttributeJoinedGroup( sqlAst, "shipAddresses", tableGroup ->
						assertThat( tableGroup.getTableGroupJoins(), isEmpty() )
					);

				}
		);
	}

	// util methods for verifying 'from-clause' ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private void assertEmptyJoinedGroup(SelectStatement sqlAst) {
		final FromClause fromClause = sqlAst.getQuerySpec().getFromClause();
		assertThat( fromClause.getRoots(), hasSize( 1 ) );

		final TableGroup rootTableGroup = fromClause.getRoots().get( 0 );
		assertThat( rootTableGroup.getTableGroupJoins(), isEmpty() );
	}

	private void assertEntityValuedJoinedGroup(SelectStatement sqlAst, String expectedAttributeName, Class<?> expectedEntityJpaClass, Consumer<TableGroup> tableGroupConsumer) {
		final FromClause fromClause = sqlAst.getQuerySpec().getFromClause();
		assertThat( fromClause.getRoots(), hasSize( 1 ) );

		final TableGroup rootTableGroup = fromClause.getRoots().get( 0 );
		assertThat( rootTableGroup.getTableGroupJoins(), hasSize( 1 ) );

		final TableGroup joinedGroup = rootTableGroup.getTableGroupJoins().iterator().next().getJoinedGroup();
		assertThat( joinedGroup.getModelPart().getPartName(), is( expectedAttributeName ) );
		assertThat( joinedGroup.getModelPart().getJavaTypeDescriptor().getJavaType(), assignableTo( expectedEntityJpaClass ) );
		assertThat( joinedGroup.getModelPart(), instanceOf( EntityValuedModelPart.class ) );

		tableGroupConsumer.accept( joinedGroup );
	}

	private void assertPluralAttributeJoinedGroup(SelectStatement sqlAst, String expectedPluralAttributeName, Consumer<TableGroup> tableGroupConsumer) {
		final FromClause fromClause = sqlAst.getQuerySpec().getFromClause();
		assertThat( fromClause.getRoots(), hasSize( 1 ) );

		final TableGroup root = fromClause.getRoots().get( 0 );
		assertThat( root.getTableGroupJoins(), hasSize( 1 ) );

		final TableGroup joinedGroup = root.getTableGroupJoins().iterator().next().getJoinedGroup();
		assertThat( joinedGroup.getModelPart().getPartName(), is( expectedPluralAttributeName ) );
		assertThat( joinedGroup.getModelPart(), instanceOf( PluralAttributeMapping.class ) );
		tableGroupConsumer.accept( joinedGroup );
	}

	private void assertPersonHomeAddressJoinedGroup(TableGroup tableGroup) {
		assertThat( tableGroup.getTableGroupJoins(), hasSize( 1 ) );

		final TableGroup joinedGroup = tableGroup.getTableGroupJoins().iterator().next().getJoinedGroup();
		assertThat( joinedGroup.getModelPart().getPartName(), is( "homeAddress" ) );
		assertThat( joinedGroup.getModelPart(), instanceOf( EmbeddedAttributeMapping.class ) );
		assertThat( joinedGroup, instanceOf( CompositeTableGroup.class ) );
	}

	// util methods for verifying 'domain-result' graph ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private void assertDomainResult(SelectStatement sqlAst,
									Class<?> expectedEntityJpaClass,
									String expectedAttributeName,
									Class<?> expectedAttributeEntityJpaClass,
									Consumer<EntityFetch> entityFetchConsumer) {
		assertThat( sqlAst.getDomainResultDescriptors(), hasSize( 1 ) );

		final DomainResult domainResult = sqlAst.getDomainResultDescriptors().get( 0 );
		assertThat( domainResult, instanceOf( EntityResult.class ) );

		final EntityResult entityResult = (EntityResult) domainResult;
		assertThat( entityResult.getReferencedModePart().getJavaTypeDescriptor().getJavaType(), assignableTo( expectedEntityJpaClass ) );
		assertThat( entityResult.getFetches(), hasSize( 1 ) );

		final Fetch fetch = entityResult.getFetches().get( 0 );
		assertThat( fetch, instanceOf( EntityFetch.class ) );

		final EntityFetch entityFetch = (EntityFetch) fetch;
		assertThat( entityFetch.getFetchedMapping().getFetchableName(), is( expectedAttributeName ) );
		assertThat( entityFetch.getReferencedModePart().getJavaTypeDescriptor().getJavaType(), assignableTo( expectedAttributeEntityJpaClass ) );

		entityFetchConsumer.accept( entityFetch );
	}

	private <T> SelectStatement buildSqlSelectAst(
			Class<T> entityType,
			RootGraphImplementor<T> entityGraph,
			GraphSemantic mode,
			SessionFactoryScope scope) {
		final EntityPersister entityDescriptor = scope.getSessionFactory().getDomainModel().getEntityDescriptor( entityType );

		final LoadQueryInfluencers loadQueryInfluencers = new LoadQueryInfluencers( scope.getSessionFactory() );
		final EffectiveEntityGraph effectiveEntityGraph = loadQueryInfluencers.getEffectiveEntityGraph();
		effectiveEntityGraph.applyGraph( entityGraph, mode );

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

		@ManyToOne(fetch = FetchType.LAZY)
		ExpressCompany company;
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
