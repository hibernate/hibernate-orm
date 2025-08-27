/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.results;

import org.hibernate.query.QueryTypeMismatchException;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@Jira( "https://hibernate.atlassian.net/browse/HHH-18401" )
@DomainModel(annotatedClasses = {SimpleEntity.class, SimpleComposite.class, Dto.class, Dto2.class})
@SessionFactory
@SuppressWarnings("JUnitMalformedDeclaration")
public class InvalidReturnTests {

	@Test
	void testCreateQuery(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			try {
				session.createQuery( Queries.ENTITY, SimpleComposite.class );
				fail( "Expecting a QueryTypeMismatchException" );
			}
			catch (QueryTypeMismatchException expected) {
			}

			try {
				session.createQuery( Queries.ENTITY, Dto.class );
				fail( "Expecting a QueryTypeMismatchException" );
			}
			catch (QueryTypeMismatchException expected) {
			}

			try {
				session.createQuery( Queries.ENTITY_NO_SELECT, SimpleComposite.class );
				fail( "Expecting a QueryTypeMismatchException" );
			}
			catch (QueryTypeMismatchException expected) {
			}

			try {
				session.createQuery( Queries.COMPOSITE, SimpleEntity.class );
				fail( "Expecting a QueryTypeMismatchException" );
			}
			catch (QueryTypeMismatchException expected) {
			}

			try {
				session.createQuery( Queries.ID_NAME_DTO, SimpleEntity.class );
				fail( "Expecting a QueryTypeMismatchException" );
			}
			catch (QueryTypeMismatchException expected) {
			}
		} );
	}

	@Test
	void testCreateSelectionQuery(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			try {
				session.createSelectionQuery( Queries.ENTITY, SimpleComposite.class );
				fail( "Expecting a QueryTypeMismatchException" );
			}
			catch (QueryTypeMismatchException expected) {
			}

			try {
				session.createSelectionQuery( Queries.ENTITY_NO_SELECT, Dto.class );
				fail( "Expecting a QueryTypeMismatchException" );
			}
			catch (QueryTypeMismatchException expected) {
			}

			try {
				session.createSelectionQuery( Queries.ENTITY_NO_SELECT, SimpleComposite.class );
				fail( "Expecting a QueryTypeMismatchException" );
			}
			catch (QueryTypeMismatchException expected) {
			}

			try {
				session.createSelectionQuery( Queries.COMPOSITE, SimpleEntity.class );
				fail( "Expecting a QueryTypeMismatchException" );
			}
			catch (QueryTypeMismatchException expected) {
			}

			try {
				session.createSelectionQuery( Queries.ID_NAME_DTO, SimpleEntity.class );
				fail( "Expecting a QueryTypeMismatchException" );
			}
			catch (QueryTypeMismatchException expected) {
			}
		} );
	}

	@Test
	void testNamedQuery(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			try {
				session.createNamedQuery( Queries.NAMED_ENTITY, SimpleComposite.class );
				fail( "Expecting a QueryTypeMismatchException" );
			}
			catch (QueryTypeMismatchException expected) {
			}

			try {
				session.createNamedQuery( Queries.NAMED_ENTITY_NO_SELECT, Dto.class );
				fail( "Expecting a QueryTypeMismatchException" );
			}
			catch (QueryTypeMismatchException expected) {
			}

			try {
				session.createNamedQuery( Queries.NAMED_ENTITY_NO_SELECT, SimpleComposite.class );
				fail( "Expecting a QueryTypeMismatchException" );
			}
			catch (QueryTypeMismatchException expected) {
			}

			try {
				session.createNamedQuery( Queries.NAMED_COMPOSITE, SimpleEntity.class );
				fail( "Expecting a QueryTypeMismatchException" );
			}
			catch (QueryTypeMismatchException expected) {
			}

			try {
				session.createNamedQuery( Queries.NAMED_ID_NAME_DTO, SimpleEntity.class );
				fail( "Expecting a QueryTypeMismatchException" );
			}
			catch (QueryTypeMismatchException expected) {
			}
		} );
	}
}
