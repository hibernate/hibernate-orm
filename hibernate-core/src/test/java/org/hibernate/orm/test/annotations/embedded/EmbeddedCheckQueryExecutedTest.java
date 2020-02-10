/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.annotations.embedded;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				EmbeddedCheckQueryExecutedTest.InternetProvider.class,
				EmbeddedCheckQueryExecutedTest.Manager.class,
		}
)
@SessionFactory(generateStatistics = true)
@ServiceRegistry(settings = {
		@ServiceRegistry.Setting(name = AvailableSettings.IMPLICIT_NAMING_STRATEGY, value = "jpa")
})
public class EmbeddedCheckQueryExecutedTest {

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createQuery( "delete from InternetProvider" ).executeUpdate();
			session.createQuery( "delete from Manager" ).executeUpdate();
		} );
	}

	@Test
	public void testEmbeddedAndOneToManyHql(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();

		InternetProvider provider = new InternetProvider();
		scope.inTransaction(
				session -> {
					LegalStructure structure = new LegalStructure();
					structure.setName( "Rogers" );
					provider.setOwner( structure );
					session.persist( provider );

					Manager manager = new Manager();
					manager.setName( "Bill" );
					manager.setEmployer( provider );
					structure.getTopManagement().add( manager );
					session.persist( manager );
				}
		);

		scope.inTransaction(
				session -> {
					statistics.clear();
					InternetProvider internetProviderQueried =
							(InternetProvider) session.createQuery(
									"from InternetProvider i join fetch i.owner o join fetch o.topManagement" )
									.uniqueResult();
					assertTrue( Hibernate.isInitialized( internetProviderQueried.getOwner().getTopManagement() ) );
					assertThat(
							internetProviderQueried.getOwner().getTopManagement().iterator().next().getEmployer(),
							is( internetProviderQueried )
					);
					assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
				}
		);

		scope.inTransaction(
				session -> {
					InternetProvider internetProvider = session.get( InternetProvider.class, provider.getId() );
					Manager manager = internetProvider.getOwner().getTopManagement().iterator().next();
					session.delete( manager );
					session.delete( internetProvider );
				}
		);
	}

	@Test
	public void testEmbeddedAndOneToManyHql2(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();

		InternetProvider provider = new InternetProvider();
		scope.inTransaction(
				session -> {
					LegalStructure structure = new LegalStructure();
					structure.setName( "Rogers" );
					provider.setOwner( structure );
					session.persist( provider );
					Manager manager = new Manager();
					manager.setName( "Bill" );
					manager.setEmployer( provider );
					structure.getTopManagement().add( manager );
					session.persist( manager );
				}
		);

		scope.inTransaction(
				session -> {
					statistics.clear();

					InternetProvider internetProviderQueried =
							(InternetProvider) session.createQuery(
									"from InternetProvider i join fetch i.owner.topManagement" )
									.uniqueResult();
					assertTrue( Hibernate.isInitialized( internetProviderQueried.getOwner().getTopManagement() ) );
					assertThat(
							internetProviderQueried.getOwner().getTopManagement().iterator().next().getEmployer(),
							is( internetProviderQueried )
					);
					assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
				}
		);

		scope.inTransaction(
				session -> {
					InternetProvider internetProvider = session.get( InternetProvider.class, provider.getId() );
					Manager manager = internetProvider.getOwner().getTopManagement().iterator().next();
					session.delete( manager );
					session.delete( internetProvider );
				}
		);
	}

	@Entity(name = "InternetProvider")
	public static class InternetProvider {
		private Integer id;
		private LegalStructure owner;

		@Id
		@GeneratedValue
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public LegalStructure getOwner() {
			return owner;
		}

		public void setOwner(LegalStructure owner) {
			this.owner = owner;
		}
	}

	@Embeddable
	public static class LegalStructure {
		private String name;
		private Set<Manager> topManagement = new HashSet<Manager>();

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@OneToMany(mappedBy = "employer")
		public Set<Manager> getTopManagement() {
			return topManagement;
		}

		public void setTopManagement(Set<Manager> topManagement) {
			this.topManagement = topManagement;
		}
	}

	@Entity(name = "Manager")
	public static class Manager {
		private Integer id;
		private String name;
		private InternetProvider employer;

		@ManyToOne
		public InternetProvider getEmployer() {
			return employer;
		}

		public void setEmployer(InternetProvider employer) {
			this.employer = employer;
		}

		@Id
		@GeneratedValue
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
