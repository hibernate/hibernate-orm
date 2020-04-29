/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.named.resultmapping;

import org.hibernate.query.named.NamedQueryRepository;
import org.hibernate.query.named.NamedResultSetMappingMemento;
import org.hibernate.query.spi.QueryEngine;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = SimpleEntityWithNamedMappings.class )
@SessionFactory
public class SimpleNamedMappingTests {
	@Test
	@FailureExpected( reason = "Memento-ization of row-reader not yet implemented" )
	public void testMapping(SessionFactoryScope sessionFactoryScope) {
		final QueryEngine queryEngine = sessionFactoryScope.getSessionFactory().getQueryEngine();
		final NamedQueryRepository namedQueryRepository = queryEngine.getNamedQueryRepository();
		final NamedResultSetMappingMemento mappingMemento = namedQueryRepository.getResultSetMappingMemento( "name" );
		assertThat( mappingMemento.toResultSetMapping(), notNullValue() );
	}

	// todo (6.0) : atm that ^^ is as far as we can test until we implement native-query support to test applying a mapping
}
