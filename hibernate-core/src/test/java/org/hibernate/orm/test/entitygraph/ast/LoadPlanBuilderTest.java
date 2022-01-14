/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.entitygraph.ast;

import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.ast.internal.CollectionLoaderSingleKey;
import org.hibernate.loader.ast.internal.SingleIdEntityLoaderStandardImpl;
import org.hibernate.loader.ast.internal.SingleIdLoadPlan;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultGraphPrinter;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.collection.internal.CollectionDomainResult;
import org.hibernate.sql.results.graph.entity.EntityResult;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.NotImplementedYet;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hibernate.testing.hamcrest.CollectionMatchers.hasSize;
import static org.hibernate.testing.hamcrest.CollectionMatchers.isEmpty;
import static org.junit.Assert.assertThat;

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
		final EntityPersister entityDescriptor = sessionFactory.getDomainModel().getEntityDescriptor( Message.class );

		final SingleIdEntityLoaderStandardImpl loader = new SingleIdEntityLoaderStandardImpl(
				entityDescriptor,
				sessionFactory
		);

		final SingleIdLoadPlan loadPlan = loader.resolveLoadPlan(
				LockOptions.READ,
				LoadQueryInfluencers.NONE,
				sessionFactory
		);

		assertThat(
				loadPlan.getJdbcSelect()
						.getJdbcValuesMappingProducer()
						.resolve( null, sessionFactory )
						.getDomainResults(),
				hasSize( 1 )

		);
		final DomainResult domainResult = loadPlan.getJdbcSelect().getJdbcValuesMappingProducer()
				.resolve( null, sessionFactory )
				.getDomainResults()
				.get( 0 );
		assertThat( domainResult, instanceOf( EntityResult.class ) );
		final EntityResult entityResult = (EntityResult) domainResult;
		assertThat( entityResult.getFetches(), hasSize( 2 ) );

		final Fetch txtFetch = entityResult.getFetches().get( 0 );

		final Fetch posterFetch = entityResult.getFetches().get( 1 );
	}

	@Test
	@NotImplementedYet(reason = "Cascade-driven DomainResult graph building not yet implemented")
	public void testCascadeBasedBuild() {
		throw new NotYetImplementedFor6Exception( "Cascade-driven DomainResult graph building not yet implemented" );
//		EntityPersister ep = (EntityPersister) sessionFactory().getClassMetadata(Message.class);
//		CascadeStyleLoadPlanBuildingAssociationVisitationStrategy strategy = new CascadeStyleLoadPlanBuildingAssociationVisitationStrategy(
//				CascadingActions.MERGE,
//				sessionFactory(),
//				LoadQueryInfluencers.NONE,
//				LockMode.NONE
//		);
//		LoadPlan plan = MetamodelDrivenLoadPlanBuilder.buildRootEntityLoadPlan( strategy, ep );
//		assertFalse( plan.hasAnyScalarReturns() );
//		assertEquals( 1, plan.getReturns().size() );
//		Return rtn = plan.getReturns().get( 0 );
//		EntityReturn entityReturn = ExtraAssertions.assertTyping( EntityReturn.class, rtn );
//		assertNotNull( entityReturn.getFetches() );
//		assertEquals( 1, entityReturn.getFetches().length );
//		Fetch fetch = entityReturn.getFetches()[0];
//		EntityFetch entityFetch = ExtraAssertions.assertTyping( EntityFetch.class, fetch );
//		assertNotNull( entityFetch.getFetches() );
//		assertEquals( 0, entityFetch.getFetches().length );
//
//		LoadPlanTreePrinter.INSTANCE.logTree( plan, new AliasResolutionContextImpl( sessionFactory() ) );
	}

	@Test
	public void testCollectionInitializerCase(SessionFactoryScope scope) {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		final EntityPersister posterEntityDescriptor = sessionFactory.getDomainModel().getEntityDescriptor( Poster.class );
		final PluralAttributeMapping messages = (PluralAttributeMapping) posterEntityDescriptor.findAttributeMapping( "messages" );

		final CollectionLoaderSingleKey loader = new CollectionLoaderSingleKey(
				messages,
				LoadQueryInfluencers.NONE,
				sessionFactory
		);

		assertThat( loader.getSqlAst().getDomainResultDescriptors(), hasSize( 1 ) );
		assertThat( loader.getSqlAst().getDomainResultDescriptors().get( 0 ), instanceOf( CollectionDomainResult.class ) );
		final CollectionDomainResult domainResult = (CollectionDomainResult) loader.getSqlAst()
				.getDomainResultDescriptors()
				.get( 0 );

		DomainResultGraphPrinter.logDomainResultGraph( loader.getSqlAst().getDomainResultDescriptors() );

		assertThat( domainResult.getFetches(), isEmpty() );
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
