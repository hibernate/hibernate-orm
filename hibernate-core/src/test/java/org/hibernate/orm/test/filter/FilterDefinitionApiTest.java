/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.filter;

import org.hibernate.SessionFactory;
import org.hibernate.UnknownFilterException;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.engine.FilterDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Verifies the application-facing filter definition and its SPI access path.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = FilterDefinitionApiTest.FilteredEntity.class)
@org.hibernate.testing.orm.junit.SessionFactory
class FilterDefinitionApiTest {

	@Test
	void metadataAndIdentity(SessionFactoryScope scope) {
		final SessionFactory factory = scope.getSessionFactory();
		final FilterDefinition definition = factory.getFilterDefinition( "byId" );
		assertThat( definition.getFilterName() ).isEqualTo( "byId" );
		assertThat( definition.getParameterNames() ).containsExactly( "id" );
		assertThat( definition.getDefaultFilterCondition() ).isEqualTo( "id = :id" );
		assertThat( definition.isAutoEnabled() ).isFalse();
		assertThat( definition.isAppliedToLoadByKey() ).isTrue();

		final var spiDefinition = factory.unwrap( SessionFactoryImplementor.class ).getFilterDefinition( "byId" );
		assertThat( spiDefinition ).isSameAs( definition );
		assertThat( spiDefinition.getParameterJdbcMapping( "id" ) ).isNotNull();
		scope.inSession( session ->
				assertThat( session.enableFilter( "byId" ).getFilterDefinition() ).isSameAs( definition ) );
		try ( var session = factory.openStatelessSession() ) {
			assertThat( session.enableFilter( "byId" ).getFilterDefinition() ).isSameAs( definition );
		}
	}

	@Test
	void unknownFilter(SessionFactoryScope scope) {
		final SessionFactory factory = scope.getSessionFactory();
		assertThatThrownBy( () -> factory.getFilterDefinition( "missing" ) )
				.isInstanceOf( UnknownFilterException.class );
	}

	@Entity(name = "DefinitionApiEntity")
	@FilterDef(name = "byId", defaultCondition = "id = :id",
			parameters = @ParamDef(name = "id", type = Integer.class), applyToLoadByKey = true)
	@org.hibernate.annotations.Filter(name = "byId")
	static class FilteredEntity {
		@Id
		Integer id;
	}
}
