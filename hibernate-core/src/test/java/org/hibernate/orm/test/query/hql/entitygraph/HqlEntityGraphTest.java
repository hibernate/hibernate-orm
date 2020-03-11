/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.hql.entitygraph;

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

import org.hibernate.engine.spi.EffectiveEntityGraph;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.orm.test.loading.entitygraph.EntityGraphLoadPlanBuilderTest;
import org.hibernate.query.hql.spi.HqlQueryImplementor;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.sqm.internal.QuerySqmImpl;
import org.hibernate.query.sqm.sql.SqmSelectTranslation;
import org.hibernate.query.sqm.sql.internal.StandardSqmSelectTranslator;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
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
 * @author Nathan Xu
 */
@DomainModel(
		annotatedClasses = {
				HqlEntityGraphTest.Cat.class,
				HqlEntityGraphTest.Person.class,
				HqlEntityGraphTest.Country.class,
				HqlEntityGraphTest.Dog.class,
				HqlEntityGraphTest.ExpressCompany.class
		}
)
@SessionFactory
@TestForIssue( jiraKey = "HHH-13756" )
public class HqlEntityGraphTest {

	@Test
	void testBasicFetchSemantics(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final RootGraphImplementor<HqlEntityGraphTest.Cat> eg = session.createEntityGraph( HqlEntityGraphTest.Cat.class );

					final SelectStatement sqlAst = buildSqlSelectAst( HqlEntityGraphTest.Cat.class, "select c from Cat c", eg, GraphSemantic.FETCH, session );

					// Check the from-clause
					assertEmptyJoinedGroup( sqlAst );

					// Check the domain-result graph
					assertDomainResult( sqlAst, HqlEntityGraphTest.Cat.class, "owner", Person.class,
										entityFetch -> assertThat( entityFetch, instanceOf( EntityFetchDelayedImpl.class ) )
					);
				}
		);
	}

	@Test
	void testFetchSemanticsWithSubgraph(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final RootGraphImplementor<HqlEntityGraphTest.Cat> eg = session.createEntityGraph( HqlEntityGraphTest.Cat.class );
					eg.addSubgraph( "owner", HqlEntityGraphTest.Person.class );

					final SelectStatement sqlAst = buildSqlSelectAst( HqlEntityGraphTest.Cat.class, "select c from Cat as c", eg, GraphSemantic.FETCH, session );

					// Check the from-clause
					assertEntityValuedJoinedGroup( sqlAst, "owner", HqlEntityGraphTest.Person.class, this::assertPersonHomeAddressJoinedGroup );

					// Check the domain-result graph
					assertDomainResult( sqlAst, HqlEntityGraphTest.Cat.class, "owner", HqlEntityGraphTest.Person.class, entityFetch -> {} );
				}
		);
	}

	@Test
	void testFetchSemanticsWithDeepSubgraph(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final RootGraphImplementor<HqlEntityGraphTest.Cat> eg = session.createEntityGraph( HqlEntityGraphTest.Cat.class );
					eg.addSubgraph( "owner", HqlEntityGraphTest.Person.class ).addSubgraph( "company", HqlEntityGraphTest.ExpressCompany.class );

					final SelectStatement sqlAst = buildSqlSelectAst( HqlEntityGraphTest.Cat.class, "select c from Cat as c", eg, GraphSemantic.FETCH, session );

					// Check the from-clause
					assertEntityValuedJoinedGroup( sqlAst, "owner", HqlEntityGraphTest.Person.class, tableGroup -> {
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
					assertDomainResult( sqlAst, HqlEntityGraphTest.Cat.class, "owner", HqlEntityGraphTest.Person.class, entityFetch -> {
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
	void testBasicLoadSemantics(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final RootGraphImplementor<HqlEntityGraphTest.Cat> eg = session.createEntityGraph( HqlEntityGraphTest.Cat.class );

					final SelectStatement sqlAst = buildSqlSelectAst( HqlEntityGraphTest.Cat.class, "select c from Cat as c", eg, GraphSemantic.LOAD, session );

					// Check the from-clause
					assertEmptyJoinedGroup( sqlAst );

					// Check the domain-result graph
					assertDomainResult( sqlAst, HqlEntityGraphTest.Cat.class, "owner", HqlEntityGraphTest.Person.class,
										entityFetch -> assertThat( entityFetch, instanceOf( EntityFetchDelayedImpl.class ) ) );
				}
		);
	}

	@Test
	void testLoadLoadPlanBuildingWithSubgraph(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final RootGraphImplementor<HqlEntityGraphTest.Cat> eg = session.createEntityGraph( HqlEntityGraphTest.Cat.class );
					eg.addSubgraph( "owner", HqlEntityGraphTest.Person.class );

					final SelectStatement sqlAst = buildSqlSelectAst( HqlEntityGraphTest.Cat.class, "select c from Cat as c", eg, GraphSemantic.LOAD, session );

					// Check the from-clause
					assertEntityValuedJoinedGroup( sqlAst, "owner", HqlEntityGraphTest.Person.class, this::assertPersonHomeAddressJoinedGroup );

					// Check the domain-result graph
					assertDomainResult( sqlAst, HqlEntityGraphTest.Cat.class, "owner", HqlEntityGraphTest.Person.class, entityFetch -> {
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
				session -> {
					final RootGraphImplementor<HqlEntityGraphTest.Dog> eg = session.createEntityGraph( HqlEntityGraphTest.Dog.class );
					eg.addAttributeNodes( "favorites" );

					final SelectStatement sqlAst = buildSqlSelectAst( HqlEntityGraphTest.Dog.class, "select d from Dog as d", eg, GraphSemantic.LOAD, session );

					// Check the from-clause
					assertPluralAttributeJoinedGroup( sqlAst, "favorites" );
				}
		);
	}

	@Test
	void testBasicElementCollectionsFetchGraph(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final RootGraphImplementor<HqlEntityGraphTest.Dog> eg = session.createEntityGraph( HqlEntityGraphTest.Dog.class );
					eg.addAttributeNodes( "favorites" );

					final SelectStatement sqlAst = buildSqlSelectAst( HqlEntityGraphTest.Dog.class, "select d from Dog as d", eg, GraphSemantic.FETCH, session );

					// Check the from-clause
					assertPluralAttributeJoinedGroup( sqlAst, "favorites" );
				}
		);
	}

	@Test
	void testEmbeddedCollectionLoadSubgraph(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final RootGraphImplementor<HqlEntityGraphTest.ExpressCompany> eg = session.createEntityGraph( HqlEntityGraphTest.ExpressCompany.class );
					eg.addAttributeNodes( "shipAddresses" );

					final SelectStatement sqlAst = buildSqlSelectAst(
							HqlEntityGraphTest.ExpressCompany.class,
							"select company from ExpressCompany as company",
							eg, GraphSemantic.LOAD,
							session
					);

					// Check the from-clause
					assertPluralAttributeJoinedGroup( sqlAst, "shipAddresses" );

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
		assertThat( joinedGroup.getModelPart(), instanceOf( EntityValuedModelPart.class ) );

		final EntityValuedModelPart entityValuedModelPart = (EntityValuedModelPart) joinedGroup.getModelPart();
		assertThat( entityValuedModelPart.getPartName(), is( expectedAttributeName ) );
		assertThat( entityValuedModelPart.getEntityMappingType().getJavaTypeDescriptor().getJavaType(), assignableTo( expectedEntityJpaClass ) );
		tableGroupConsumer.accept( joinedGroup );
	}

	private void assertPluralAttributeJoinedGroup(SelectStatement sqlAst, String expectedPluralAttributeName) {
		final FromClause fromClause = sqlAst.getQuerySpec().getFromClause();
		assertThat( fromClause.getRoots(), hasSize( 1 ) );

		final TableGroup root = fromClause.getRoots().get( 0 );
		assertThat( root.getTableGroupJoins(), hasSize( 1 ) );

		final TableGroup joinedGroup = root.getTableGroupJoins().iterator().next().getJoinedGroup();
		assertThat( joinedGroup.getModelPart(), instanceOf( PluralAttributeMapping.class ) );

		final PluralAttributeMapping pluralAttributeMapping = (PluralAttributeMapping) joinedGroup.getModelPart();
		assertThat( pluralAttributeMapping.getAttributeName(), is( expectedPluralAttributeName ) );
		assertThat( joinedGroup.getTableGroupJoins(), isEmpty() );
	}

	private void assertPersonHomeAddressJoinedGroup(TableGroup tableGroup) {
		assertThat( tableGroup.getTableGroupJoins(), hasSize( 1 ) );

		final TableGroupJoin tableGroupJoin = tableGroup.getTableGroupJoins().iterator().next();
		assertThat( tableGroupJoin.getJoinedGroup(), instanceOf( CompositeTableGroup.class ) );

		final CompositeTableGroup compositeTableGroup = (CompositeTableGroup) tableGroupJoin.getJoinedGroup();
		assertThat( compositeTableGroup.getModelPart(), instanceOf( EmbeddedAttributeMapping.class ) );

		final EmbeddedAttributeMapping embeddedAttributeMapping = (EmbeddedAttributeMapping) compositeTableGroup.getModelPart();
		assertThat( embeddedAttributeMapping.getPartName(), is( "homeAddress" ) );
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
			String hql,
			RootGraphImplementor<T> entityGraph,
			GraphSemantic mode,
			SessionImplementor session) {

		final LoadQueryInfluencers loadQueryInfluencers = new LoadQueryInfluencers( session.getSessionFactory() );
		final EffectiveEntityGraph effectiveEntityGraph = loadQueryInfluencers.getEffectiveEntityGraph();
		effectiveEntityGraph.applyGraph( entityGraph, mode );

		final QueryImplementor<T> query = session.createQuery(
				hql,
				entityType
		);
		final HqlQueryImplementor<String> hqlQuery = (HqlQueryImplementor<String>) query;

		final SqmSelectStatement<T> sqmStatement = (SqmSelectStatement<T>) hqlQuery.getSqmStatement();

		final StandardSqmSelectTranslator sqmConverter = new StandardSqmSelectTranslator(
				hqlQuery.getQueryOptions(),
				( (QuerySqmImpl) hqlQuery ).getDomainParameterXref(),
				query.getParameterBindings(),
				loadQueryInfluencers,
				session.getSessionFactory()
		);

		final SqmSelectTranslation sqmInterpretation = sqmConverter.translate( sqmStatement );
		return sqmInterpretation.getSqlAst();
	}

	@Entity(name = "Dog")
	public static class Dog {
		@Id
		String name;

		@ElementCollection
		Set<String> favorites;
	}

	@Entity(name = "Cat")
	public static class Cat {
		@Id
		String name;

		@ManyToOne(fetch = FetchType.LAZY)
		Person owner;
	}

	@Entity(name = "Person")
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

	@Entity(name = "ExpressCompany")
	public static class ExpressCompany {
		@Id
		String name;

		@ElementCollection
		Set<Address> shipAddresses;
	}

	@Entity(name = "Country")
	public static class Country {
		@Id
		String name;
	}
}


