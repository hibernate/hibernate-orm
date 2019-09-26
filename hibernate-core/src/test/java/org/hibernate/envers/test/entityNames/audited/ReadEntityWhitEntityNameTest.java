/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entitynames.audited;

import java.util.List;

import org.hibernate.envers.test.EnversSingleSessionBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.entitynames.audited.Person;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author Hern&aacute;n Chanfreau
 */
@Disabled("NYI - ClassCastException - IdentifierGeneratorHelper$2 cannot be cast to java.lang.Long during unwrap")
public class ReadEntityWhitEntityNameTest extends EnversSingleSessionBasedFunctionalTest {
	private long id_pers1;
	private long id_pers2;
	private long id_pers3;

	private Person currentPers1;

	@Override
	protected String[] getMappings() {
		return new String[] { "entityNames/audited/mappings.hbm.xml" };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		// The session used by all test methods will be initialized here first
		inTransactions(
				// Revision 1
				session -> {
					final Person pers1 = new Person( "Herman", 28 );
					session.persist( "Personaje", pers1 );
					id_pers1 = pers1.getId();
				},

				// Revision 2
				session -> {
					final Person pers1 = (Person) session.get( "Personaje", id_pers1 );
					pers1.setAge( 29 );
					session.persist( "Personaje", pers1 );

					final Person pers2 = new Person( "Leandro", 29 );
					session.persist( "Personaje", pers2 );
					id_pers2 = pers2.getId();
				},

				// Revision 3
				session -> {
					final Person pers1 = (Person) session.get( "Personaje", id_pers1 );
					pers1.setName( "Hernan David" );

					final Person pers2 = (Person) session.get( "Personaje", id_pers2 );
					pers2.setAge( 30 );

					session.persist( "Personaje", pers1 );
					session.persist( "Personaje", pers2 );

					final Person pers3 = new Person( "Barba", 30 );
					session.persist( "Personaje", pers3 );
					id_pers3 = pers3.getId();
				},

				// Get snapshot of Person in separate transaction
				session -> {
					currentPers1 = (Person) session.get( "Personaje", id_pers1 );
				}
		);
	}

	@DynamicTest
	public void testRetrieveRevisionsWithEntityName() {
		List<Number> pers1Revs = getAuditReader().getRevisions( Person.class, "Personaje", id_pers1 );
		List<Number> pers2Revs = getAuditReader().getRevisions( Person.class, "Personaje", id_pers2 );
		List<Number> pers3Revs = getAuditReader().getRevisions( Person.class, "Personaje", id_pers3 );

		assertThat( pers1Revs, CollectionMatchers.hasSize( 3 ) );
		assertThat( pers2Revs, CollectionMatchers.hasSize( 2 ) );
		assertThat( pers3Revs, CollectionMatchers.hasSize( 1 ) );
	}

	@DynamicTest
	public void testRetrieveAuditedEntityWithEntityName() {
		assertThat( getAuditReader().find( Person.class, "Personaje", id_pers1, 1 ), notNullValue() );
		assertThat( getAuditReader().find( Person.class, "Personaje", id_pers1, 2 ), notNullValue() );
		assertThat( getAuditReader().find( Person.class, "Personaje", id_pers1, 3 ), notNullValue() );
	}

	@DynamicTest
	public void testObtainEntityNameAuditedEntityWithEntityName() {
		final Person person1_1 = getAuditReader().find( Person.class, "Personaje", id_pers1, 1 );
		final Person person1_2 = getAuditReader().find( Person.class, "Personaje", id_pers1, 2 );
		final Person person1_3 = getAuditReader().find( Person.class, "Personaje", id_pers1, 3 );

		final String personName = inSession( session -> { return session.getEntityName( currentPers1 ); } );

		assertThat( getAuditReader().getEntityName( person1_1.getId(), 1, person1_1 ), equalTo( personName ) );
		assertThat( getAuditReader().getEntityName( person1_2.getId(), 2, person1_2 ), equalTo( personName ) );
		assertThat( getAuditReader().getEntityName( person1_3.getId(), 3, person1_3 ), equalTo( personName ) );

	}

	@DynamicTest
	public void testRetrieveAuditedEntityWithEntityNameWithNewSession() {
		// force a new session and AR
		forceNewSession();

		assertThat( getAuditReader().find( Person.class, "Personaje", id_pers1, 1 ), notNullValue() );
		assertThat( getAuditReader().find( Person.class, "Personaje", id_pers1, 2 ), notNullValue() );
		assertThat( getAuditReader().find( Person.class, "Personaje", id_pers1, 3 ), notNullValue() );
	}
}
