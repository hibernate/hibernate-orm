/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.foreigngenerator;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {Thing.class, Info.class})
@SessionFactory
public class ForeignGeneratorTests {
	@Test
	void checkGeneration(SessionFactoryScope sessionFactoryScope) {
		sessionFactoryScope.inTransaction( (session) -> {
			final Thing thing = new Thing( 1, "thing-1" );
			final Info info = new Info( thing, "info-1" );
			thing.setInfo( info );
			info.setOwner( thing );
			session.persist( thing );
			session.persist( info );
		} );

		sessionFactoryScope.inTransaction( (session) -> {
			final Thing thing = session.find( Thing.class, 1 );
			assertThat( thing.getId() ).isEqualTo( thing.getInfo().getId() );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
}
