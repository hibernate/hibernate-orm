/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph.ast;

import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.ast.internal.CollectionLoaderSingleKey;
import org.hibernate.loader.ast.internal.SingleIdEntityLoaderStandardImpl;
import org.hibernate.loader.ast.internal.SingleIdLoadPlan;
import org.hibernate.loader.ast.spi.CascadingFetchProfile;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultGraphPrinter;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.collection.internal.CollectionDomainResult;
import org.hibernate.sql.results.graph.entity.EntityResult;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.LockMode.READ;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = { LoadPlanBuilderTest.Message.class, LoadPlanBuilderTest.Poster.class }
)
@SessionFactory
public class LoadPlanBuilderTest {
	@Test
	public void testSimpleBuild(SessionFactoryScope scope) {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		final EntityPersister entityDescriptor = sessionFactory.getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( Message.class );

		final SingleIdEntityLoaderStandardImpl<?> loader = new SingleIdEntityLoaderStandardImpl<>( entityDescriptor, new LoadQueryInfluencers( sessionFactory ) );

		final SingleIdLoadPlan<?> loadPlan = loader.resolveLoadPlan(
				new LockOptions( READ ),
				new LoadQueryInfluencers( sessionFactory )
		);

		final List<DomainResult<?>> domainResults = loadPlan.getJdbcSelect()
				.getJdbcValuesMappingProducer()
				.resolve( null, new LoadQueryInfluencers( sessionFactory ), sessionFactory )
				.getDomainResults();

		assertThat( domainResults ).hasSize( 1 );
		final DomainResult<?> domainResult = domainResults.get( 0 );
		assertThat( domainResult ).isInstanceOf( EntityResult.class );
		final EntityResult entityResult = (EntityResult) domainResult;
		assertThat( entityResult.getFetches() ).hasSize( 2 );

		final Fetch txtFetch = entityResult.getFetches().get( entityDescriptor.findAttributeMapping( "msgTxt" ) );
		assertThat( txtFetch ).isNotNull();
		assertThat( txtFetch.getFetchedMapping().getFetchableName() ).isEqualTo( "msgTxt" );
		assertThat( txtFetch.getTiming() ).isEqualTo( FetchTiming.IMMEDIATE );

		final Fetch posterFetch = entityResult.getFetches().get( entityDescriptor.findAttributeMapping( "poster" ) );
		assertThat( posterFetch ).isNotNull();
		assertThat( posterFetch.getFetchedMapping().getFetchableName() ).isEqualTo( "poster" );
		assertThat( posterFetch.getTiming() ).isEqualTo( FetchTiming.DELAYED );
	}

	@Test
	public void testCascadeBasedBuild(SessionFactoryScope scope) {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		final EntityPersister entityDescriptor = (EntityPersister) sessionFactory.getRuntimeMetamodels().getEntityMappingType( Message.class );

		final SingleIdEntityLoaderStandardImpl<?> loader = new SingleIdEntityLoaderStandardImpl<>( entityDescriptor, new LoadQueryInfluencers( sessionFactory ) );

		final LoadQueryInfluencers influencers = new LoadQueryInfluencers( sessionFactory ) {
			@Override
			public CascadingFetchProfile getEnabledCascadingFetchProfile() {
				return CascadingFetchProfile.MERGE;
			}
		};

		final SingleIdLoadPlan<?> loadPlan = loader.resolveLoadPlan(
				new LockOptions( READ ),
				influencers
		);
		final List<DomainResult<?>> domainResults = loadPlan.getJdbcSelect()
				.getJdbcValuesMappingProducer()
				.resolve( null, new LoadQueryInfluencers( sessionFactory ), sessionFactory )
				.getDomainResults();

		assertThat( domainResults ).hasSize( 1 );
		final DomainResult<?> domainResult = domainResults.get( 0 );
		assertThat( domainResult ).isInstanceOf( EntityResult.class );
		final EntityResult entityResult = (EntityResult) domainResult;
		assertThat( entityResult.getFetches() ).hasSize( 2 );

		final Fetch txtFetch = entityResult.getFetches().get( entityDescriptor.findAttributeMapping( "msgTxt" ) );
		assertThat( txtFetch ).isNotNull();
		assertThat( txtFetch.getFetchedMapping().getFetchableName() ).isEqualTo( "msgTxt" );
		assertThat( txtFetch.getTiming() ).isEqualTo( FetchTiming.IMMEDIATE );

		final Fetch posterFetch = entityResult.getFetches().get( entityDescriptor.findAttributeMapping( "poster" ) );
		assertThat( posterFetch ).isNotNull();
		assertThat( posterFetch.getFetchedMapping().getFetchableName() ).isEqualTo( "poster" );
		assertThat( posterFetch.getTiming() ).isEqualTo( FetchTiming.IMMEDIATE );
	}

	@Test
	public void testCollectionInitializerCase(SessionFactoryScope scope) {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		final EntityPersister posterEntityDescriptor = sessionFactory.getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( Poster.class );
		final PluralAttributeMapping messages = (PluralAttributeMapping) posterEntityDescriptor.findAttributeMapping( "messages" );

		final CollectionLoaderSingleKey loader = new CollectionLoaderSingleKey(
				messages,
				new LoadQueryInfluencers( sessionFactory ),
				sessionFactory
		);

		assertThat( loader.getSqlAst().getDomainResultDescriptors() ).hasSize( 1 );
		assertThat( loader.getSqlAst().getDomainResultDescriptors().get( 0 ) ).isInstanceOf( CollectionDomainResult.class );
		final CollectionDomainResult domainResult = (CollectionDomainResult) loader.getSqlAst()
				.getDomainResultDescriptors()
				.get( 0 );

		DomainResultGraphPrinter.logDomainResultGraph( loader.getSqlAst().getDomainResultDescriptors() );

		assertThat( domainResult.getFetches() ).isEmpty();
	}

	@Entity( name = "Message" )
	public static class Message {
		@Id
		private Integer mid;
		private String msgTxt;
		@ManyToOne( fetch = FetchType.LAZY, cascade = CascadeType.MERGE )
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
