/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import java.time.LocalDate;
import jakarta.persistence.Tuple;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.contacts.Contact;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Beikov
 * @author Jan-Willem Gmelig Meyling
 * @author Sayra Ranjha
 */
@ServiceRegistry
@DomainModel(standardModels = StandardDomainModel.CONTACTS)
@SessionFactory( useCollectingStatementInspector = true )
public class GroupByTest {

	@Test
	@JiraKey( value = "HHH-1615")
	public void testGroupByEntity(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "select e, count(*) from Contact e group by e", Tuple.class ).list();
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-9301" )
	public void testGroupByAliasedBasicPart(SessionFactoryScope scope) {
		final SQLStatementInspector sqlStatementInspector = scope.getCollectingStatementInspector();
		sqlStatementInspector.clear();

		scope.inSession( (session) -> {
			final String qryString = "select c.id as id_alias, count(1) as occurrences"
					+ " from Contact c"
					+ " group by id_alias"
					+ " order by id_alias";
			final Tuple result = session.createQuery( qryString, Tuple.class ).uniqueResult();
			assertThat( result ).isNotNull();
			assertThat( result.get( "id_alias" ) ).isEqualTo( 123 );
			assertThat( result.get( "occurrences" ) ).isEqualTo( 1L );

			assertThat( sqlStatementInspector.getSqlQueries() ).hasSize( 1 );
			assertThat( sqlStatementInspector.getSqlQueries().get( 0 ) ).isNotNull();
		} );
	}

	@Test
	@JiraKey( value = "HHH-9301" )
	public void testGroupByAliasedCompositePart(SessionFactoryScope scope) {
		final SQLStatementInspector sqlStatementInspector = scope.getCollectingStatementInspector();
		sqlStatementInspector.clear();

		scope.inTransaction( (session) -> {
			final String qryString = "select c.name as name_alias, count(1) as occurrences"
					+ " from Contact c"
					+ " group by name_alias"
					+ " order by name_alias";
			final Tuple result = session.createQuery( qryString, Tuple.class ).uniqueResult();
			assertThat( result ).isNotNull();
			assertThat( result.get( "name_alias" ) ).isInstanceOf( Contact.Name.class );
			final Contact.Name name = result.get( "name_alias", Contact.Name.class );
			assertThat( name.getFirst() ).isEqualTo( "Johnny" );
			assertThat( name.getLast() ).isEqualTo( "Lawrence" );
			assertThat( result.get( "occurrences" ) ).isEqualTo( 1L );

			assertThat( sqlStatementInspector.getSqlQueries() ).hasSize( 1 );
			assertThat( sqlStatementInspector.getSqlQueries().get( 0 ) ).isNotNull();
		} );
	}


	@Test
	@JiraKey( value = "HHH-9301" )
	public void testGroupByMultipleAliases(SessionFactoryScope scope) {
		final SQLStatementInspector sqlStatementInspector = scope.getCollectingStatementInspector();
		sqlStatementInspector.clear();

		scope.inTransaction( (session) -> {
			final String qryString = "select c.id as id_alias, c.gender as gender_alias, count(1) as occurrences"
					+ " from Contact c"
					+ " group by id_alias, gender_alias"
					+ " order by id_alias, gender_alias";
			final Tuple result = session.createQuery( qryString, Tuple.class ).uniqueResult();
			assertThat( result ).isNotNull();
			assertThat( result.get( "id_alias" ) ).isEqualTo( 123 );
			assertThat( result.get( "gender_alias" ) ).isEqualTo( Contact.Gender.MALE );
			assertThat( result.get( "occurrences" ) ).isEqualTo( 1L );

			assertThat( sqlStatementInspector.getSqlQueries() ).hasSize( 1 );
			assertThat( sqlStatementInspector.getSqlQueries().get( 0 ) ).isNotNull();
		} );
	}

	@BeforeEach
	public void prepareData(SessionFactoryScope scope) {
		scope.inTransaction( (em) -> {
			Contact entity1 = new Contact( 123, new Contact.Name( "Johnny", "Lawrence" ), Contact.Gender.MALE, LocalDate.of(1970, 1, 1) );
			em.persist( entity1 );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

}
