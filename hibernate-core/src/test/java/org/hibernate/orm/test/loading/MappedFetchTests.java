/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.loading;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.ast.internal.LoaderSelectBuilder;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.collection.internal.DelayedCollectionFetch;
import org.hibernate.sql.results.graph.collection.internal.EagerCollectionFetch;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.entity.EntityResult;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.internal.ImmutableFetchList;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
				MappedFetchTests.RootEntity.class,
				MappedFetchTests.SimpleEntity.class
		}
)
@SessionFactory
public class MappedFetchTests {
	@Test
	public void baseline(SessionFactoryScope scope) {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		final MappingMetamodel domainModel = sessionFactory.getRuntimeMetamodels().getMappingMetamodel();
		final EntityPersister rootEntityDescriptor = domainModel.getEntityDescriptor( RootEntity.class );

		final SelectStatement sqlAst = LoaderSelectBuilder.createSelect(
				rootEntityDescriptor,
				null,
				rootEntityDescriptor.getIdentifierMapping(),
				null,
				1,
				new LoadQueryInfluencers( sessionFactory ),
				new LockOptions(),
				jdbcParameter -> {
				},
				sessionFactory
		);

		assertThat( sqlAst.getDomainResultDescriptors().size(), is( 1 ) );

		final DomainResult domainResult = sqlAst.getDomainResultDescriptors().get( 0 );
		assertThat( domainResult, instanceOf( EntityResult.class ) );

		final EntityResult entityResult = (EntityResult) domainResult;
		final ImmutableFetchList fetches = entityResult.getFetches();

		// name + both lists
		assertThat( fetches.size(), is( 3 ) );

		// order is alphabetical...

		final Fetch nameFetch = fetches.get( rootEntityDescriptor.findAttributeMapping( "name" ) );
		assertThat( nameFetch.getFetchedMapping().getFetchableName(), is( "name" ) );
		assertThat( nameFetch, instanceOf( BasicFetch.class ) );

		final Fetch nickNamesFetch = fetches.get( rootEntityDescriptor.findAttributeMapping( "nickNames" ) );
		assertThat( nickNamesFetch.getFetchedMapping().getFetchableName(), is( "nickNames" ) );
		assertThat( nickNamesFetch, instanceOf( EagerCollectionFetch.class ) );

		final Fetch simpleEntitiesFetch = fetches.get( rootEntityDescriptor.findAttributeMapping( "simpleEntities" ) );
		assertThat( simpleEntitiesFetch.getFetchedMapping().getFetchableName(), is( "simpleEntities" ) );
		assertThat( simpleEntitiesFetch, instanceOf( DelayedCollectionFetch.class ) );


		final QuerySpec querySpec = sqlAst.getQuerySpec();

		final TableGroup tableGroup = querySpec.getFromClause().getRoots().get( 0 );
		assertThat( tableGroup.getModelPart(), is( rootEntityDescriptor ) );
		assertThat( tableGroup.getTableGroupJoins().size(), is( 1  ) );

		final TableGroupJoin collectionJoin = tableGroup.getTableGroupJoins().iterator().next();
		assertThat( collectionJoin.getJoinedGroup().getModelPart(), is( nickNamesFetch.getFetchedMapping() ) );

		assertThat( collectionJoin.getPredicate(), notNullValue() );
		assertThat( collectionJoin.getPredicate(), instanceOf( ComparisonPredicate.class ) );
	}

	@Test
	public void smokeTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final RootEntity first = session.get( RootEntity.class, 1 );
					assertThat( Hibernate.isInitialized( first ), is( true ) );

					assertThat( Hibernate.isInitialized( first.getNickNames() ), is( true ) );
					assertThat( first.getNickNames().size(), is( 2 ) );

					assertThat( Hibernate.isInitialized( first.getSimpleEntities() ), is( false ) );

					final RootEntity second = session.get( RootEntity.class, 2 );
					assertThat( Hibernate.isInitialized( second ), is( true ) );

					assertThat( Hibernate.isInitialized( second.getNickNames() ), is( true ) );
					assertThat( second.getNickNames().size(), is( 2 ) );

					assertThat( Hibernate.isInitialized( second.getSimpleEntities() ), is( false ) );
				}
		);
	}

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final RootEntity root1 = new RootEntity( 1, "first" );
					root1.addNickName( "1st" );
					root1.addNickName( "primo" );
					session.persist( root1 );

					final RootEntity root2 = new RootEntity( 2, "second" );
					root2.addNickName( "2nd" );
					root2.addNickName( "first loser" );
					session.persist( root2 );
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity( name = "RootEntity" )
	@Table( name = "root_entity" )
	public static class RootEntity {
		private Integer id;
		private String name;

		private List<String> nickNames;
		private List<SimpleEntity> simpleEntities;

		public RootEntity() {
		}

		public RootEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@ElementCollection( fetch = FetchType.EAGER )
		@CollectionTable( name = "nick_names" )
		public List<String> getNickNames() {
			return nickNames;
		}

		public void setNickNames(List<String> nickNames) {
			this.nickNames = nickNames;
		}

		public void addNickName(String name) {
			if ( nickNames == null ) {
				nickNames = new ArrayList<>();
			}
			nickNames.add( name );
		}

		@OneToMany( fetch = FetchType.LAZY )
		@JoinColumn( name = "simple_id" )
		public List<SimpleEntity> getSimpleEntities() {
			return simpleEntities;
		}

		public void setSimpleEntities(List<SimpleEntity> simpleEntities) {
			this.simpleEntities = simpleEntities;
		}

		public void addSimpleEntity(SimpleEntity simpleEntity) {
			if ( simpleEntities == null ) {
				simpleEntities = new ArrayList<>();
			}
			simpleEntities.add( simpleEntity );
		}
	}

	@Entity( name = "SimpleEntity" )
	@Table( name = "simple_entity" )
	public static class SimpleEntity {
		private Integer id;
		private String name;

		public SimpleEntity() {
		}

		public SimpleEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}


}
