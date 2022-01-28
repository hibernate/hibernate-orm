/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.hibernate.ScrollMode;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.graph.spi.AppliedGraph;
import org.hibernate.query.IllegalSelectQueryException;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.SqmQuery;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.contacts.Contact;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		standardModels = StandardDomainModel.CONTACTS,
		annotatedClasses = BasicSelectionQueryTests.DummyEntity.class
)
@SessionFactory
public class BasicSelectionQueryTests {
	@Test
	public void typedEntitySelectTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final SelectionQuery<Contact> query = session.createSelectionQuery( "select c from Contact c", Contact.class );
			checkResults( query, session );
		} );
	}

	@Test
	public void rawEntitySelectTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			// its unbounded
			final SelectionQuery<?> query = session.createSelectionQuery( "select c from Contact c" );
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

	@Test
	@SuppressWarnings("unchecked")
	public void unwrapTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final SelectionQuery<Contact> query =
					session.createSelectionQuery( "select c from Contact c", Contact.class )
							.unwrap( SelectionQuery.class );
			checkResults( query, session );
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
