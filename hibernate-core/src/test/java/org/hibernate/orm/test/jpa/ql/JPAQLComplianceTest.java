/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.ql;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.query.Query;
import org.hibernate.query.SemanticException;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.orm.test.jpa.model.AbstractJPATest;
import org.hibernate.orm.test.jpa.model.Item;
import org.junit.jupiter.api.Test;

import static org.hibernate.testing.orm.junit.ExtraAssertions.assertTyping;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for various JPAQL compliance issues
 *
 * @author Steve Ebersole
 */
public class JPAQLComplianceTest extends AbstractJPATest {

	@Test
	public void testAliasNameSameAsUnqualifiedEntityName() {
		inTransaction(
				session -> {
					session.createQuery( "select item from Item item" ).list();
					session.createQuery( "select item from Item item where item.name = 'a'" ).list();

				}
		);
	}

	@Test
	public void testIdentifierCaseSensitive() {
		// a control test (a user reported that the JPA 'case insensitivity' support
		// caused problems with the "discriminator resolution" code; unable to reproduce)...
		inSession(
				session -> {
					session.createQuery( "select E from MyEntity e where other.class = MySubclassEntity" );
					session.createQuery( "select e from MyEntity e where e.other.class = MySubclassEntity" );
					session.createQuery( "select e from MyEntity E where e.class = MySubclassEntity" );

					session.createQuery( "select object(I) from Item i" ).list();
				}
		);
	}

	@Test
	public void testIdentifierCasesensitivityAndDuplicateFromElements() {
		inSession(
				session ->
						session.createQuery(
								"select e from MyEntity e where exists (select 1 from MyEntity e2 where e2.other.name  = 'something' and e2.other.other = e)" )
		);
	}

	@Test
	public void testGeneratedSubquery() {
		inSession(
				session ->
						session.createQuery( "select c FROM Item c WHERE c.parts IS EMPTY" ).list()

		);
	}

	@Test
	public void testOrderByAlias() {
		inSession(
				session -> {
					session.createQuery( "select c.name as myname FROM Item c ORDER BY myname" ).list();
					session.createQuery(
									"select p.name as name, p.stockNumber as stockNo, p.unitPrice as uPrice FROM Part p ORDER BY name, abs( p.unitPrice ), stockNo" )
							.list();
				} );
	}

	@Test
	@JiraKey(value = "HHH-12290")
	public void testParametersMixturePositionalAndNamed() {
		inTransaction(
				s -> {
					try {
						s.createQuery( "select item from Item item where item.id = ?1 and item.name = :name" ).list();
						fail( "Expecting QuerySyntaxException because of named and positional parameters mixture" );
					}
					catch (IllegalArgumentException e) {
						assertNotNull( e.getCause() );
						assertTyping( SemanticException.class, e.getCause() );
					}
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-12290")
	public void testParametersMixtureNamedAndPositional() {
		inTransaction(
				s -> {
					try {
						s.createQuery( "select item from Item item where item.id = :id and item.name = ?1" ).list();
						fail( "Expecting QuerySyntaxException because of named and positional parameters mixture" );
					}
					catch (IllegalArgumentException e) {
						assertNotNull( e.getCause() );
						assertTyping( SemanticException.class, e.getCause() );
					}
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-12290")
	public void testReusedNamedCollectionParam() {
		inTransaction(
				session -> {
					Query q = session.createQuery(
							"select e from MyEntity e where e.surname in (:values) or e.name in (:values)" );
					List<String> params = new ArrayList<>();
					params.add( "name" );
					params.add( "other" );
					q.setParameter( "values", params );
					q.list();
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-12290")
	public void testReusedPositionalCollectionParam() {
		inTransaction(
				session -> {
					Query q = session.createQuery( "select e from MyEntity e where e.name in (?1) or e.surname in (?1)" );
					List<String> params = new ArrayList<>();
					params.add( "name" );
					params.add( "other" );
					q.setParameter( 1, params );
					q.list();
				}
		);
	}

	/**
	 * Positional collection parameter is expanded to the list of named parameters. In spite of this fact, initial query
	 * query is wrong in terms of JPA and exception must be thrown
	 */
	@Test
	@JiraKey(value = "HHH-12290")
	public void testParametersMixtureNamedCollectionAndPositional() {
		inTransaction(
				s -> {
					try {
						Query q = s.createQuery(
								"select item from Item item where item.id in (?1) and item.name = :name" );
						List<Long> params = new ArrayList<>();
						params.add( 0L );
						params.add( 1L );
						q.setParameter( 1, params );
						q.setParameter( "name", "name" );
						q.list();
						fail( "Expecting QuerySyntaxException because of named and positional parameters mixture" );
					}
					catch (IllegalArgumentException e) {
						assertNotNull( e.getCause() );
						assertTyping( SemanticException.class, e.getCause() );
					}
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-12290")
	public void testParameterCollectionParenthesesAndPositional() {
		final Item item = new Item( "Mouse" );
		final Item item2 = new Item( "Computer" );

		inTransaction(
				s -> {
					s.persist( item );
					s.persist( item2 );
				}
		);

		inTransaction(
				s -> {
					Query q = s.createQuery(
							"select item from Item item where item.id in(?1) and item.name in (?2) and item.id in(?1)" );

					List<Long> idParams = new ArrayList<>();
					idParams.add( item.getId() );
					idParams.add( item2.getId() );
					q.setParameter( 1, idParams );

					List<String> nameParams = new ArrayList<>();
					nameParams.add( item.getName() );
					nameParams.add( item2.getName() );
					q.setParameter( 2, nameParams );

					List result = q.getResultList();
					assertNotNull( result );
					assertEquals( 2, result.size() );
				}
		);

		inTransaction(
				s -> s.createQuery( "select i from Item i" ).list().forEach( result -> s.remove( result ) )
		);

	}
}
