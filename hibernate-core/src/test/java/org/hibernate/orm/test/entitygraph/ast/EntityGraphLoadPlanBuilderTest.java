/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph.ast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.EffectiveEntityGraph;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.loader.ast.internal.LoaderSelectBuilder;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.AttributeMappingsList;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.tree.from.FromClause;
import org.hibernate.sql.ast.tree.from.LazyTableGroup;
import org.hibernate.sql.ast.tree.from.StandardVirtualTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.collection.internal.DelayedCollectionFetch;
import org.hibernate.sql.results.graph.embeddable.internal.EmbeddableFetchImpl;
import org.hibernate.sql.results.graph.entity.EntityFetch;
import org.hibernate.sql.results.graph.entity.EntityResult;
import org.hibernate.sql.results.graph.entity.internal.EntityDelayedFetchImpl;
import org.hibernate.sql.results.graph.entity.internal.EntityFetchJoinedImpl;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SessionFactoryScopeAware;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.platform.commons.util.CollectionUtils;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.hamcrest.AssignableMatcher.assignableTo;
import static org.hibernate.testing.hamcrest.CollectionMatchers.hasSize;
import static org.hibernate.testing.hamcrest.CollectionMatchers.isEmpty;

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
@JiraKey( value = "HHH-13756" )
public class EntityGraphLoadPlanBuilderTest implements SessionFactoryScopeAware {

	private SessionFactoryScope scope;

	@Override
	public void injectSessionFactoryScope(SessionFactoryScope scope) {
		this.scope = scope;
	}

	@ParameterizedTest
	@EnumSource( GraphSemantic.class )
	void testBasicLoadPlanBuilding(GraphSemantic graphSemantic) {
		scope.inTransaction(
				em -> {
					final RootGraphImplementor<Cat> eg = em.createEntityGraph( Cat.class );

					final SelectStatement sqlAst = buildSqlSelectAst( Cat.class, eg, graphSemantic, scope );

					// Check the from-clause
					assertEmptyJoinedGroup( sqlAst );

					// Check the domain-result graph
					assertDomainResult( sqlAst, Cat.class, "owner", Person.class,
										entityFetch -> assertThat( entityFetch, instanceOf( EntityDelayedFetchImpl.class ) )
					);
				}
		);
	}

	@ParameterizedTest
	@EnumSource( GraphSemantic.class )
	void testLoadPlanBuildingWithSubgraph(GraphSemantic graphSemantic) {
		scope.inTransaction(
				em -> {
					final RootGraphImplementor<Cat> eg = em.createEntityGraph( Cat.class );
					eg.addSubgraph( "owner", Person.class );

					final SelectStatement sqlAst = buildSqlSelectAst( Cat.class, eg, graphSemantic, scope );

					// Check the from-clause
					assertEntityValuedJoinedGroup( sqlAst, "owner", Person.class, this::assertPersonHomeAddressJoinedGroup );

					// Check the domain-result graph
					assertDomainResult( sqlAst, Cat.class, "owner", Person.class, entityFetch -> {
						if ( graphSemantic == GraphSemantic.LOAD ) {
							assertThat( entityFetch, instanceOf( EntityFetchJoinedImpl.class ) );
							final EntityResult entityResult = ( (EntityFetchJoinedImpl) entityFetch ).getEntityResult();
							final Map<String, Class<? extends Fetch>> fetchClassByAttributeName = entityResult.getFetches().stream().collect( Collectors.toMap(
									fetch -> fetch.getFetchedMapping().getPartName(),
									Fetch::getClass
							) );
							final Map<String, Class<? extends Fetch>> expectedFetchClassByAttributeName = new HashMap<>();
							expectedFetchClassByAttributeName.put( "pets", DelayedCollectionFetch.class );
							expectedFetchClassByAttributeName.put( "homeAddress", EmbeddableFetchImpl.class );
							expectedFetchClassByAttributeName.put( "company", EntityDelayedFetchImpl.class );
							assertThat( fetchClassByAttributeName, is( expectedFetchClassByAttributeName ) );
						}
					} );
				}
		);
	}

	@Test
	void testFetchLoadPlanBuildingWithDeepSubgraph() {
		scope.inTransaction(
				em -> {
					final RootGraphImplementor<Cat> eg = em.createEntityGraph( Cat.class );
					eg.addSubgraph( "owner", Person.class ).addSubgraph( "company", ExpressCompany.class );

					final SelectStatement sqlAst = buildSqlSelectAst( Cat.class, eg, GraphSemantic.FETCH, scope );

					// Check the from-clause
					assertEntityValuedJoinedGroup( sqlAst, "owner", Person.class, tableGroup -> {
						List<TableGroupJoin> tableGroupJoins = tableGroup.getTableGroupJoins();
						Map<String, Class<? extends TableGroup>> tableGroupByName = tableGroupJoins.stream()
								.map( TableGroupJoin::getJoinedGroup )
								.collect( Collectors.toMap(
										tg -> tg.getModelPart().getPartName(),
										TableGroup::getClass
								) );
						Map<String, Class<? extends TableGroup> > expectedTableGroupByName = new HashMap<>();
						expectedTableGroupByName.put( "homeAddress", StandardVirtualTableGroup.class );
						expectedTableGroupByName.put( "company", LazyTableGroup.class );
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

						Fetchable fetchable = getFetchable( "company", Person.class );
						final Fetch companyFetch = ownerEntityResult.findFetch( fetchable );
						assertThat( companyFetch, notNullValue() );

						final EntityResult companyEntityResult = ( (EntityFetchJoinedImpl) companyFetch).getEntityResult();
						assertThat( companyEntityResult.getFetches().size(), is( 1 ) );

						final Fetch shipAddressesFetch = companyEntityResult.getFetches().iterator().next();
						assertThat( shipAddressesFetch.getFetchedMapping().getPartName(), is( "shipAddresses" ) );
						assertThat( shipAddressesFetch, instanceOf( DelayedCollectionFetch.class ) );
					} );
				}
		);
	}

	private Fetchable getFetchable(String attributeName, Class entityClass) {
		EntityPersister person = scope.getSessionFactory().getRuntimeMetamodels().getMappingMetamodel().findEntityDescriptor(
				entityClass.getName() );
		AttributeMappingsList attributeMappings = person.getAttributeMappings();
		Fetchable fetchable = null;
		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			AttributeMapping mapping = attributeMappings.get( i );
			if ( mapping.getAttributeName().equals( attributeName ) ) {
				fetchable = mapping;
			}
		}
		return fetchable;
	}

	@ParameterizedTest
	@EnumSource( GraphSemantic.class )
	void testBasicElementCollections(GraphSemantic graphSemantic) {
		scope.inTransaction(
				em -> {
					final RootGraphImplementor<Dog> eg = em.createEntityGraph( Dog.class );
					eg.addAttributeNodes( "favorites" );

					final SelectStatement sqlAst = buildSqlSelectAst( Dog.class, eg, graphSemantic, scope );

					// Check the from-clause
					assertPluralAttributeJoinedGroup( sqlAst, "favorites", tableGroup -> {} );
				}
		);
	}

	@ParameterizedTest
	@EnumSource( GraphSemantic.class )
	void testEmbeddedCollectionLoadGraph(GraphSemantic graphSemantic) {
		scope.inTransaction(
				em -> {
					final RootGraphImplementor<ExpressCompany> eg = em.createEntityGraph( ExpressCompany.class );
					eg.addAttributeNodes( "shipAddresses" );

					final SelectStatement sqlAst = buildSqlSelectAst(
							ExpressCompany.class,
							eg, graphSemantic,
							scope
					);

					// Check the from-clause
					assertPluralAttributeJoinedGroup( sqlAst, "shipAddresses", tableGroup -> {
						if ( graphSemantic == GraphSemantic.LOAD ) {
							assertThat( tableGroup.getTableGroupJoins(), hasSize( 1 ) );
							assertThat( tableGroup.getNestedTableGroupJoins(), isEmpty() );

							final TableGroup compositeTableGroup = CollectionUtils.getOnlyElement( tableGroup.getTableGroupJoins() )
									.getJoinedGroup();
							assertThat( compositeTableGroup, instanceOf( StandardVirtualTableGroup.class ) );
							assertThat( compositeTableGroup.getTableGroupJoins(), hasSize( 1 ) );
							assertThat( compositeTableGroup.getNestedTableGroupJoins(), isEmpty() );

							final TableGroup countryTableGroup = CollectionUtils.getOnlyElement( compositeTableGroup.getTableGroupJoins() )
									.getJoinedGroup();
							assertThat( countryTableGroup.getModelPart().getPartName(), is( "country" ) );

							assertThat( countryTableGroup.getTableGroupJoins(), isEmpty() );
							assertThat( countryTableGroup.getNestedTableGroupJoins(), isEmpty() );
						}
						else {
							assertThat( tableGroup.getTableGroupJoins(), hasSize( 1 ) );
							assertThat( tableGroup.getNestedTableGroupJoins(), isEmpty() );

							final TableGroup compositeTableGroup = CollectionUtils.getOnlyElement( tableGroup.getTableGroupJoins() ).getJoinedGroup();
							assertThat( compositeTableGroup, instanceOf( StandardVirtualTableGroup.class ) );
							assertThat( compositeTableGroup.getNestedTableGroupJoins(), isEmpty() );
							assertThat( compositeTableGroup.getTableGroupJoins(), hasSize( 1 ) );

							final TableGroup joinedGroup = compositeTableGroup.getTableGroupJoins().get( 0 ).getJoinedGroup();
							assertThat( joinedGroup.isInitialized(), is( false ) );
						}
					} );

				}
		);
	}

	// util methods for verifying 'from-clause' ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private void assertEmptyJoinedGroup(SelectStatement sqlAst) {
		final FromClause fromClause = sqlAst.getQuerySpec().getFromClause();
		assertThat( fromClause.getRoots(), hasSize( 1 ) );

		final TableGroup rootTableGroup = fromClause.getRoots().get( 0 );
		assertThat( rootTableGroup.getTableGroupJoins(), hasSize( 1 ) );

		final TableGroup tableGroup = rootTableGroup.getTableGroupJoins().get( 0 ).getJoinedGroup();
		assertThat( tableGroup.isInitialized(), is( false ) );
	}

	private void assertEntityValuedJoinedGroup(SelectStatement sqlAst, String expectedAttributeName, Class<?> expectedEntityJpaClass, Consumer<TableGroup> tableGroupConsumer) {
		final FromClause fromClause = sqlAst.getQuerySpec().getFromClause();
		assertThat( fromClause.getRoots(), hasSize( 1 ) );

		final TableGroup rootTableGroup = fromClause.getRoots().get( 0 );
		assertThat( rootTableGroup.getTableGroupJoins(), hasSize( 1 ) );

		final TableGroup joinedGroup = CollectionUtils.getOnlyElement( rootTableGroup.getTableGroupJoins() ).getJoinedGroup();
		assertThat( joinedGroup.getModelPart().getPartName(), is( expectedAttributeName ) );
		assertThat( joinedGroup.getModelPart().getJavaType().getJavaTypeClass(), assignableTo( expectedEntityJpaClass ) );
		assertThat( joinedGroup.getModelPart(), instanceOf( EntityValuedModelPart.class ) );

		tableGroupConsumer.accept( joinedGroup );
	}

	private void assertPluralAttributeJoinedGroup(SelectStatement sqlAst, String expectedPluralAttributeName, Consumer<TableGroup> tableGroupConsumer) {
		final FromClause fromClause = sqlAst.getQuerySpec().getFromClause();
		assertThat( fromClause.getRoots(), hasSize( 1 ) );

		final TableGroup root = fromClause.getRoots().get( 0 );
		assertThat( root.getTableGroupJoins(), hasSize( 1 ) );

		final TableGroup joinedGroup = CollectionUtils.getOnlyElement( root.getTableGroupJoins() ).getJoinedGroup();
		assertThat( joinedGroup.getModelPart().getPartName(), is( expectedPluralAttributeName ) );
		assertThat( joinedGroup.getModelPart(), instanceOf( PluralAttributeMapping.class ) );
		tableGroupConsumer.accept( joinedGroup );
	}

	private void assertPersonHomeAddressJoinedGroup(TableGroup tableGroup) {
		assertThat( tableGroup.getTableGroupJoins(), hasSize( 2 ) );

		final TableGroup company = tableGroup.getTableGroupJoins().get( 0 ).getJoinedGroup();
		assertThat( company.getModelPart().getPartName(), is( "company" ) );
		assertThat( company.getModelPart(), instanceOf( ToOneAttributeMapping.class ) );
		assertThat( company, instanceOf( LazyTableGroup.class ) );
		assertThat( company.isInitialized(), is( false ) );

		final TableGroup homeAddress = tableGroup.getTableGroupJoins().get( 1 ).getJoinedGroup();
		assertThat( homeAddress.getModelPart().getPartName(), is( "homeAddress" ) );
		assertThat( homeAddress.getModelPart(), instanceOf( EmbeddedAttributeMapping.class ) );
		assertThat( homeAddress, instanceOf( StandardVirtualTableGroup.class ) );
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
		assertThat( entityResult.getReferencedModePart().getJavaType().getJavaTypeClass(), assignableTo( expectedEntityJpaClass ) );
		assertThat( entityResult.getFetches().size(), is( 1 ) );

		final Fetch fetch = entityResult.getFetches().iterator().next();
		assertThat( fetch, instanceOf( EntityFetch.class ) );

		final EntityFetch entityFetch = (EntityFetch) fetch;
		assertThat( entityFetch.getFetchedMapping().getFetchableName(), is( expectedAttributeName ) );
		assertThat( entityFetch.getReferencedModePart().getJavaType().getJavaTypeClass(), assignableTo( expectedAttributeEntityJpaClass ) );

		entityFetchConsumer.accept( entityFetch );
	}

	private <T> SelectStatement buildSqlSelectAst(
			Class<T> entityType,
			RootGraphImplementor<T> entityGraph,
			GraphSemantic mode,
			SessionFactoryScope scope) {
		final EntityPersister entityDescriptor = scope.getSessionFactory().getRuntimeMetamodels().getMappingMetamodel().getEntityDescriptor( entityType );

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
