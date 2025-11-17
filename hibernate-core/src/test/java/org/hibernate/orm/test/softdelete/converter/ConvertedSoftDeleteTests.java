/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.softdelete.converter;

import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.orm.test.softdelete.MappingVerifier;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = TheEntity.class)
@SessionFactory( useCollectingStatementInspector = true)
public class ConvertedSoftDeleteTests {
	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void verifySchema(SessionFactoryScope scope) {
		final MappingMetamodelImplementor metamodel = scope.getSessionFactory().getMappingMetamodel();

		MappingVerifier.verifyMapping(
				metamodel.getEntityDescriptor( TheEntity.class ).getSoftDeleteMapping(),
				"deleted",
				"the_entity",
				'Y'
		);
	}

	@Test
	void testUsage(SessionFactoryScope scope) {
		final SQLStatementInspector sqlInspector = scope.getCollectingStatementInspector();
		sqlInspector.clear();

		scope.inTransaction( (session) -> {
			session.persist( new TheEntity( 1, "it" ) );
		} );

		assertThat( sqlInspector.getSqlQueries() ).hasSize( 1 );
		assertThat( sqlInspector.getSqlQueries().get( 0 ) ).contains( "'N'" );
		assertThat( sqlInspector.getSqlQueries().get( 0 ) ).doesNotContain( "'Y'" );

		sqlInspector.clear();

		scope.inTransaction( (session) -> {
			final TheEntity reference = session.getReference( TheEntity.class, 1 );
			session.remove( reference );
		} );

		assertThat( sqlInspector.getSqlQueries() ).hasSize( 1 );
		assertThat( sqlInspector.getSqlQueries().get( 0 ) ).doesNotContainIgnoringCase( "delete " );
		assertThat( sqlInspector.getSqlQueries().get( 0 ) ).containsIgnoringCase( "update " );
		assertThat( sqlInspector.getSqlQueries().get( 0 ) ).containsAnyOf( "deleted='Y'", "deleted=N'Y'" );
	}
}
