/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm.untyped;

import org.hibernate.ScrollMode;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.SelectionQuery;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.contacts.Contact;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@DomainModel( standardModels = StandardDomainModel.CONTACTS )
@SessionFactory
public class BasicUntypedQueryTests {
	@Test
	public void typedEntitySelectTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			checkResults(
					session.createSelectQuery( "select c from Contact c", Contact.class ),
					session
			);
		} );
	}

	@Test
	public void rawEntitySelectTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			checkResults(
					session.createSelectQuery( "select c from Contact c" ),
					session
			);
		} );
	}

	@Test
	public void rawScalarSelectTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			checkResults(
					session.createSelectQuery( "select c.name from Contact c" ),
					session
			);
		} );
	}

	@Test
	public void typedScalarSelectTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			checkResults(
					session.createSelectQuery( "select c.name from Contact c", Contact.Name.class ),
					session
			);
			checkResults(
					session.createSelectQuery( "select c.name.first from Contact c", String.class ),
					session
			);
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
}
