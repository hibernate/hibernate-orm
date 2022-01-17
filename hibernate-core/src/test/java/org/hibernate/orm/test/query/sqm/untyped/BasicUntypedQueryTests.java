/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm.untyped;

import org.hibernate.ScrollMode;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.JpaQuery;
import org.hibernate.query.UntypedQuery;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@DomainModel( standardModels = StandardDomainModel.CONTACTS )
@SessionFactory
public class BasicUntypedQueryTests {
	@Test
	public void untypedEntitySelectTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final UntypedQuery untypedQuery = session.createUntypedQuery( "select c from Contact c" );
			checkResults( untypedQuery, session );
		} );
	}

	private void checkResults(UntypedQuery untypedQuery, SessionImplementor session) {
		untypedQuery.list();
		untypedQuery.getResultList();
		untypedQuery.uniqueResult();
		untypedQuery.uniqueResultOptional();
		untypedQuery.scroll().close();
		untypedQuery.scroll( ScrollMode.SCROLL_SENSITIVE ).close();
		untypedQuery.stream().close();
	}

	@Test
	public void untypedScalarSelectTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final UntypedQuery untypedQuery = session.createUntypedQuery( "select c.name from Contact c" );
			checkResults( untypedQuery, session );
		} );
	}
}
