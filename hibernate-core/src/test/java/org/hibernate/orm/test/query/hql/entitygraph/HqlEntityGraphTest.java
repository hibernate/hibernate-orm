/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.hql.entitygraph;

import java.util.Set;
import java.util.function.Consumer;
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
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.entity.EntityFetch;
import org.hibernate.sql.results.graph.entity.EntityResult;
import org.hibernate.sql.results.graph.entity.internal.EntityFetchDelayedImpl;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
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
					assertDomainResult( sqlAst, HqlEntityGraphTest.Cat.class, "owner", EntityGraphLoadPlanBuilderTest.Person.class,
										entityFetch -> assertThat( entityFetch, instanceOf( EntityFetchDelayedImpl.class ) )
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
		EntityGraphLoadPlanBuilderTest.Person owner;
	}

	@Entity
	public static class Person {
		@Id
		String name;

		@OneToMany(mappedBy = "owner")
		Set<EntityGraphLoadPlanBuilderTest.Cat> pets;

		@Embedded
		EntityGraphLoadPlanBuilderTest.Address homeAddress;

		@ManyToOne(fetch = FetchType.LAZY)
		EntityGraphLoadPlanBuilderTest.ExpressCompany company;
	}

	@Embeddable
	public static class Address {
		@ManyToOne
		EntityGraphLoadPlanBuilderTest.Country country;
	}

	@Entity
	public static class ExpressCompany {
		@Id
		String name;

		@ElementCollection
		Set<EntityGraphLoadPlanBuilderTest.Address> shipAddresses;
	}

	@Entity
	public static class Country {
		@Id
		String name;
	}
}


