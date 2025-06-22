/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.sqm;

import java.util.List;
import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;

import org.hibernate.ScrollMode;
import org.hibernate.dialect.HANADialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.IllegalSelectQueryException;
import org.hibernate.query.SelectionQuery;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.contacts.Contact;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		standardModels = StandardDomainModel.CONTACTS,
		annotatedClasses = BasicSelectionQueryTests.DummyEntity.class
)
@SessionFactory
@SkipForDialect( dialectClass = HANADialect.class, reason = "HANA does not support scrollable results")
public class BasicSelectionQueryTests {
	@Test
	public void typedEntitySelectTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final SelectionQuery<DummyEntity> query = session.createSelectionQuery( "select c from DummyEntity c", DummyEntity.class );
			checkResults( query, session );
		} );
	}

	@Test
	public void rawEntitySelectTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			// its unbounded
			final SelectionQuery<?> query = session.createSelectionQuery( "select c from DummyEntity c" );
			checkResults( query, session );
		} );
	}

	@Test
	public void typedScalarSelectTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final SelectionQuery<Contact.Name> nameQuery = session.createSelectionQuery( "select c.name from Contact c", Contact.Name.class );
			checkResults( nameQuery, session );

			final SelectionQuery<String> firstNameQuery = session.createSelectionQuery( "select c.name.first from Contact c", String.class );
			checkResults( firstNameQuery, session );
		} );
	}

	private void createDummyEntity(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( new DummyEntity( 1, "whatever" ) );
		} );
	}

	private final String tuple_selection_hql = "select c.id as id, c.name as name from DummyEntity c";

	@Test
	public void tupleSelectionTestBaseline(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			checkResults( session.createQuery( tuple_selection_hql, Tuple.class ), session );
		} );
	}

	@Test
	public void tupleSelectionTest(SessionFactoryScope scope) {
		createDummyEntity( scope );

		// first make sure we get back the correct results via list
		scope.inTransaction( (session) -> {
			final SelectionQuery<Tuple> selectionQuery = session.createSelectionQuery( tuple_selection_hql, Tuple.class );

			final List<Tuple> tuples = selectionQuery.list();
			assertThat( tuples ).hasSize( 1 );

			assertThat( tuples.get( 0 ) ).isInstanceOf( Tuple.class );
			final Tuple tuple = tuples.get( 0 );
			assertThat( tuple.get( 0 ) ).isEqualTo( 1 );
			assertThat( tuple.get( "id" ) ).isEqualTo( 1 );
			assertThat( tuple.get( 1 ) ).isEqualTo( "whatever" );
			assertThat( tuple.get( "name" ) ).isEqualTo( "whatever" );
		} );

		// next make sure the rest of the execution methods work
		scope.inTransaction( (session) -> {
			final SelectionQuery<Tuple> selectionQuery = session.createSelectionQuery( tuple_selection_hql, Tuple.class );
			checkResults( selectionQuery, session );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void rawScalarSelectTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final SelectionQuery<?> nameQuery = session.createSelectionQuery( "select c.name from Contact c" );
			checkResults( nameQuery, session );

			final SelectionQuery<?> firstNameQuery = session.createSelectionQuery( "select c.name.first from Contact c" );
			checkResults( firstNameQuery, session );
		} );
	}

	@Test
	public void typesNamedSelectTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final SelectionQuery<Contact.Name> nameQuery = session.createNamedSelectionQuery( "hql-name", Contact.Name.class );
			checkResults( nameQuery, session );

			final SelectionQuery<String> firstNameQuery = session.createNamedSelectionQuery( "hql-first-name", String.class );
			checkResults( firstNameQuery, session );
		} );
	}

	@Test
	public void rawNamedSelectTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final SelectionQuery<?> nameQuery = session.createNamedSelectionQuery( "hql-name" );
			checkResults( nameQuery, session );

			final SelectionQuery<?> firstNameQuery = session.createNamedSelectionQuery( "hql-first-name" );
			checkResults( firstNameQuery, session );
		} );
	}

	@Test
	public void mutationAsSelectTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			try {
				session.createSelectionQuery( "delete from Contact" );
				fail( "Expecting IllegalSelectQueryException" );
			}
			catch (IllegalSelectQueryException expected) {
			}
		} );
	}

	@Test
	public void namedMutationAsSelectTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			try {
				session.createNamedSelectionQuery( "hql-mutation" );
				fail( "Expecting IllegalSelectQueryException" );
			}
			catch (IllegalSelectQueryException expected) {
			}
		} );
	}

	private void checkResults(SelectionQuery<?> query, SessionImplementor session) {
		query.list();
		query.getResultList();
		query.uniqueResult();
		query.uniqueResultOptional();
		query.scroll().close();
		query.scroll( ScrollMode.SCROLL_SENSITIVE ).close();
		query.stream().close();
	}

	@NamedQuery( name = "hql-name", query = "select c.name from Contact c" )
	@NamedQuery( name = "hql-first-name", query = "select c.name.first from Contact c" )
	@NamedQuery( name = "hql-mutation", query = "delete from Contact" )
	@Entity( name = "DummyEntity" )
	@Table( name = "DummyEntity" )
	public static class DummyEntity {
		@Id
		private Integer id;
		@Basic
		private String name;

		private DummyEntity() {
			// for use by Hibernate
		}

		public DummyEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
