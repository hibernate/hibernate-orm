/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.annotations.embedded;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import org.hamcrest.CoreMatchers;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				EmbeddedCircularFetchTests.LeafEntity.class,
				EmbeddedCircularFetchTests.RootEntity.class
		}
)
@SessionFactory
@ServiceRegistry(settings = {
		@ServiceRegistry.Setting(name = AvailableSettings.IMPLICIT_NAMING_STRATEGY, value = "jpa"),
		@ServiceRegistry.Setting(name = AvailableSettings.GENERATE_STATISTICS, value = "true")
})
public class EmbeddedCircularFetchTests {
//	@Test
//	@TestForIssue(jiraKey = "HHH-9642")
//	public void testEmbeddedAndOneToManyHql(SessionFactoryScope scope) {
//		scope.inTransaction(
//				session -> {
//					InternetProvider provider = new InternetProvider();
//					provider.setBrandName( "Fido" );
//					LegalStructure structure = new LegalStructure();
//					structure.setCountry( "Canada" );
//					structure.setName( "Rogers" );
//					provider.setOwner( structure );
//					session.persist( provider );
//					Manager manager = new Manager();
//					manager.setName( "Bill" );
//					manager.setEmployer( provider );
//					structure.getTopManagement().add( manager );
//					session.persist( manager );
//				}
//		);
//
//		scope.inTransaction(
//				session -> {
//					InternetProvider internetProviderQueried =
//							(InternetProvider) session.createQuery( "from InternetProvider" ).uniqueResult();
//					assertFalse( Hibernate.isInitialized( internetProviderQueried.getOwner().getTopManagement() ) );
//
//				}
//		);
//
//		scope.inTransaction(
//				session -> {
//					InternetProvider internetProviderQueried =
//							(InternetProvider) session.createQuery(
//									"from InternetProvider i join fetch i.owner.topManagement" )
//									.uniqueResult();
//					assertTrue( Hibernate.isInitialized( internetProviderQueried.getOwner().getTopManagement() ) );
//
//				}
//		);
//
//		InternetProvider provider = scope.fromTransaction(
//				session -> {
//					InternetProvider internetProviderQueried =
//							(InternetProvider) session.createQuery(
//									"from InternetProvider i join fetch i.owner o join fetch o.topManagement" )
//									.uniqueResult();
//					assertTrue( Hibernate.isInitialized( internetProviderQueried.getOwner().getTopManagement() ) );
//					return internetProviderQueried;
//				}
//		);
//
//		scope.inTransaction(
//				session -> {
//					InternetProvider internetProvider = session.get( InternetProvider.class, provider.getId() );
//					Manager manager = internetProvider.getOwner().getTopManagement().iterator().next();
//					session.delete( manager );
//					session.delete( internetProvider );
//				}
//		);
//	}

	@Test
	@TestForIssue(jiraKey = "HHH-9642")
	public void testCircularFetchAcrossComponent(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					RootEntity root = new RootEntity();
					root.id = 1;
					session.save( root );

					LeafEntity leafEntity1 = new LeafEntity();
					leafEntity1.id = 10;

					LeafEntity leafEntity2 = new LeafEntity();
					leafEntity2.id = 11;

					session.save( leafEntity1 );
					session.save( leafEntity2 );

					leafEntity2.rootEntity = root;
					leafEntity1.rootEntity = root;
					root.intermediateComponent = new IntermediateComponent();

					root.intermediateComponent.leaves = new HashSet<>(  );
					root.intermediateComponent.leaves.add( leafEntity1 );
					root.intermediateComponent.leaves.add( leafEntity2 );
				}
		);

//		scope.inTransaction(
//				session -> {
//					InternetProvider internetProviderQueried =
//							(InternetProvider) session.createQuery( "from InternetProvider" ).uniqueResult();
//					assertFalse( Hibernate.isInitialized( internetProviderQueried.getOwner().getTopManagement() ) );
//
//				}
//		);

		scope.inTransaction(
				session -> {
					session.getSessionFactory().getStatistics().clear();
					final RootEntity result = session.createQuery(
							"from RootEntity r join fetch r.intermediateComponent.leaves",
							RootEntity.class
					).uniqueResult();
					assertTrue( Hibernate.isInitialized( result.getIntermediateComponent().getLeaves() ) );
					assertThat( result.getIntermediateComponent().getLeaves().size(), is( 1 ) );

					assertThat( session.getSessionFactory().getStatistics().getPrepareStatementCount(), is( 1 ) );
				}
		);

//		InternetProvider provider = scope.fromTransaction(
//				session -> {
//					InternetProvider internetProviderQueried =
//							(InternetProvider) session.createQuery(
//									"from InternetProvider i join fetch i.owner o join fetch o.topManagement" )
//									.uniqueResult();
//					LegalStructure owner = internetProviderQueried.getOwner();
//					assertTrue( Hibernate.isInitialized( owner ));
//					assertTrue( Hibernate.isInitialized( owner.getTopManagement() ) );
//					return internetProviderQueried;
//				}
//		);
//
//		scope.inTransaction(
//				session -> {
//					InternetProvider internetProvider = session.get( InternetProvider.class, provider.getId() );
//					Manager manager = internetProvider.getOwner().getTopManagement().iterator().next();
//					session.delete( manager );
//					session.delete( internetProvider );
//				}
//		);
	}

	@Entity( name = "RootEntity" )
	@Table( name = "root" )
	public static class RootEntity {
		private Integer id;
		private IntermediateComponent intermediateComponent;

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@Embedded
		public IntermediateComponent getIntermediateComponent() {
			return intermediateComponent;
		}

		public void setIntermediateComponent(IntermediateComponent intermediateComponent) {
			this.intermediateComponent = intermediateComponent;
		}
	}

	@Embeddable
	public static class IntermediateComponent {
		private String name;
		private Set<LeafEntity> leaves;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@OneToMany( mappedBy = "rootEntity" )
		public Set<LeafEntity> getLeaves() {
			return leaves;
		}

		public void setLeaves(Set<LeafEntity> leaves) {
			this.leaves = leaves;
		}
	}

	@Entity( name = "LeafEntity" )
	@Table( name = "leaf" )
	public static class LeafEntity {
		private Integer id;
		private String name;
		private RootEntity rootEntity;

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

		@ManyToOne
		@JoinColumn
		public RootEntity getRootEntity() {
			return rootEntity;
		}

		public void setRootEntity(RootEntity rootEntity) {
			this.rootEntity = rootEntity;
		}
	}
}
