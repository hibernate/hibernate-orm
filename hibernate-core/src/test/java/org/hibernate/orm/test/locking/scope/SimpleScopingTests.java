/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking.scope;

import jakarta.persistence.LockModeType;
import org.hibernate.Hibernate;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.NotImplementedYet;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {Book.class, Person.class, Publisher.class})
@SessionFactory(useCollectingStatementInspector = true)
@Jira( "https://hibernate.atlassian.net/browse/HHH-19336" )
@Jira( "https://hibernate.atlassian.net/browse/HHH-19459" )
public class SimpleScopingTests {
	@BeforeEach
	void createTestData(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final Person milton = new Person( 1, "John Milton" );
			session.persist( milton );
			final Person campbell = new Person( 2, "Joseph Campbell" );
			session.persist( campbell );
			final Person king = new Person( 3, "Stephen King" );
			session.persist( king );
			final Person straub = new Person( 4, "Peter Straub" );
			session.persist( straub );
			final Person doe = new Person( 5, "John Doe" );
			session.persist( doe );

			final Publisher acme = new Publisher( 1, "Acme Publishing House", doe );
			session.persist( acme );

			final Book paradiseLost = new Book( 1, "Paradise Lost", "Narrative poem, in the epic style, ..." );
			paradiseLost.addAuthor( milton );
			session.persist( paradiseLost );

			final Book thePowerOfMyth = new Book( 2, "The Power of Myth",
					"A look at the themes and symbols of ancient narratives ..." );
			thePowerOfMyth.addAuthor( campbell );
			session.persist( thePowerOfMyth );

			final Book theTalisman = new Book( 3, "The Talisman", "Epic of the struggle between good and evil ...", acme );
			theTalisman.addAuthor( king );
			theTalisman.addAuthor( straub );
			session.persist( theTalisman );

			final Book theDarkTower = new Book( 4, "The Dark Tower", "The epic final to the series ...", acme );
			theDarkTower.addAuthor( king );
			session.persist( theDarkTower );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	void testLoading(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final Book paradiseLost = session.find( Book.class, 1 );
			assertThat( paradiseLost.getTitle() ).isEqualTo( "Paradise Lost" );
			assertThat( Hibernate.isInitialized( paradiseLost.getAuthors() ) ).isFalse();

			final Book talisman = session.find( Book.class, 3 );
			assertThat( talisman.getTitle() ).isEqualTo( "The Talisman" );
			assertThat( Hibernate.isInitialized( talisman.getAuthors() ) ).isFalse();
		} );
	}

	@Test
	@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsTableLocking.class )
	@NotImplementedYet(reason = "Proper, strict-JPA locking not yet implemented", expectedVersion = "7.1")
	void testSimpleLocking(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			final Book theTalisman = session.find( Book.class, 3, LockModeType.PESSIMISTIC_WRITE );
			assertThat( Hibernate.isInitialized( theTalisman ) ).isTrue();
			assertThat( Hibernate.isInitialized( theTalisman.getPublisher() ) ).isTrue();
			assertThat( Hibernate.isInitialized( theTalisman.getAuthors() ) ).isFalse();
			assertThat( Hibernate.isInitialized( theTalisman.getPublisher().getLeadEditor() ) ).isFalse();
			assertThat( Hibernate.isInitialized( theTalisman.getPublisher().getBooks() ) ).isFalse();
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );

			// todo: this is going to depend on the specific database
			assertThat( sqlCollector.getSqlQueries().get( 0 ) ).contains( " for update of " );
		} );
	}
}
