/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dynamicmap;

import org.hibernate.RemovalsMode;
import org.hibernate.OrderingMode;
import org.hibernate.ReadOnlyMode;
import org.hibernate.SessionCheckMode;
import org.hibernate.graph.RootGraph;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(xmlMappings = "org/hibernate/orm/test/dynamicmap/artist.xml")
@SessionFactory
public class FindOperationTests {
	@BeforeEach
	void setUp(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final Map<String,Object> entity = new HashMap<>();
			entity.put( "id", 1 );
			entity.put( "name", "Led Zepplin" );
			session.persist( "artist", entity );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	void testSimpleFind(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final Object artist = session.find( "artist", 1 );
			checkResult( artist );

		} );
	}

	@Test
	void testFindWithOptions(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final Object artist = session.find( "artist", 1, ReadOnlyMode.READ_ONLY );
			checkResult( artist );
			assertThat( session.isReadOnly( artist ) ).isTrue();
		} );
	}

	@Test
	void testFindWithIllegalOptions(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			assertThrows( IllegalArgumentException.class, () ->session.find( "artist", 1, SessionCheckMode.ENABLED ) );
			assertThrows( IllegalArgumentException.class, () ->session.find( "artist", 1, OrderingMode.ORDERED ) );
			assertThrows( IllegalArgumentException.class, () ->session.find( "artist", 1, RemovalsMode.INCLUDE ) );
		} );
	}

	@Test
	void testFindWithGraph(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final RootGraph<Map<String, ?>> artistGraph = session.getFactory().createGraphForDynamicEntity( "artist" );
			artistGraph.addAttributeNodes( "id", "name" );
			final Object artist = session.find( artistGraph, 1, ReadOnlyMode.READ_ONLY );
			checkResult( artist );
			assertThat( session.isReadOnly( artist ) ).isTrue();
		} );
	}

	private void checkResult(Object incoming) {
		assertThat( incoming ).isNotNull();

		//noinspection unchecked
		final Map<String, Object> artist = (Map<String, Object>) incoming;
		assertThat( artist.get( "id" ) ).isEqualTo( 1 );
		assertThat( artist.get( "name" ) ).isEqualTo( "Led Zepplin" );
	}
}
