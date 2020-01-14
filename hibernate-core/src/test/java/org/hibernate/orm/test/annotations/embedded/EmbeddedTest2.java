/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.annotations.embedded;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				InternetProvider.class,
				CorpType.class,
				Nationality.class,
				Manager.class
		}
)
@SessionFactory
@ServiceRegistry(settings = {
		@ServiceRegistry.Setting(name = AvailableSettings.IMPLICIT_NAMING_STRATEGY, value = "jpa")
})
public class EmbeddedTest2 {
	@Test
	@TestForIssue(jiraKey = "HHH-9642")
	public void testEmbeddedAndOneToManyHql(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					InternetProvider provider = new InternetProvider();
					provider.setBrandName( "Fido" );
					LegalStructure structure = new LegalStructure();
					structure.setCountry( "Canada" );
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
					InternetProvider internetProviderQueried =
							(InternetProvider) session.createQuery( "from InternetProvider" ).uniqueResult();
					assertFalse( Hibernate.isInitialized( internetProviderQueried.getOwner().getTopManagement() ) );

				}
		);

		scope.inTransaction(
				session -> {
					InternetProvider internetProviderQueried =
							(InternetProvider) session.createQuery(
									"from InternetProvider i join fetch i.owner.topManagement" )
									.uniqueResult();
					assertTrue( Hibernate.isInitialized( internetProviderQueried.getOwner().getTopManagement() ) );

				}
		);

		InternetProvider provider = scope.fromTransaction(
				session -> {
					InternetProvider internetProviderQueried =
							(InternetProvider) session.createQuery(
									"from InternetProvider i join fetch i.owner o join fetch o.topManagement" )
									.uniqueResult();
					assertTrue( Hibernate.isInitialized( internetProviderQueried.getOwner().getTopManagement() ) );
					return internetProviderQueried;
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
	@TestForIssue(jiraKey = "HHH-9642")
	public void testEmbeddedAndOneToManyHqlFetch(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					InternetProvider provider = new InternetProvider();
					provider.setBrandName( "Fido" );
					LegalStructure structure = new LegalStructure();
					structure.setCountry( "Canada" );
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
					InternetProvider internetProviderQueried =
							(InternetProvider) session.createQuery(
									"from InternetProvider i join fetch i.owner.topManagement" )
									.uniqueResult();
					assertTrue( Hibernate.isInitialized( internetProviderQueried.getOwner().getTopManagement() ) );

				}
		);

		InternetProvider provider = scope.fromTransaction(
				session -> {
					InternetProvider internetProviderQueried =
							(InternetProvider) session.createQuery(
									"from InternetProvider i join fetch i.owner o join fetch o.topManagement" )
									.uniqueResult();
					LegalStructure owner = internetProviderQueried.getOwner();
					assertTrue( Hibernate.isInitialized( owner ));
					assertTrue( Hibernate.isInitialized( owner.getTopManagement() ) );
					return internetProviderQueried;
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
}
