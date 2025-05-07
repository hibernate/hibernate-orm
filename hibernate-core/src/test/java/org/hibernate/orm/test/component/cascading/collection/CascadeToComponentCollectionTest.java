/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.component.cascading.collection;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry
@DomainModel(xmlMappings = "org/hibernate/orm/test/component/cascading/collection/Mappings.xml")
@SessionFactory
public class CascadeToComponentCollectionTest {
	@AfterEach
	void dropData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testMerging(SessionFactoryScope factoryScope) {
		// step1, we create a definition with one value
		factoryScope.inTransaction( (session) -> {
			Definition definition = new Definition( 1, "greeting" );
			Value value1 = new Value( 1, definition );
			value1.getLocalizedStrings().addString( new Locale( "en_US" ), "hello" );
			session.persist( definition );

		} );

		// step2, we verify that the definition has one value; then we detach it
		Definition detached = factoryScope.fromTransaction( (session) -> {
			Definition definition = session.find( Definition.class, 1 );
			assertThat( definition.getValues() ).hasSize( 1 );
			return definition;
		} );

		// step3, we add a new value during detachment
		Value value2 = new Value( 2, detached );
		value2.getLocalizedStrings().addString( new Locale( "es" ), "hola" );

		// step4 we merge the definition
		factoryScope.inTransaction( (session) -> {
			session.merge( detached );
		} );

		// step5, final test
		factoryScope.inTransaction( (session) -> {
			Definition definition = session.find( Definition.class, 1 );
			assertThat( definition.getValues() ).hasSize( 2 );
			for ( Value v : definition.getValues() ) {
				assertThat( v.getLocalizedStrings().makeStringsCopy() ).hasSize( 1 );
			}
		} );
	}

	@SuppressWarnings("unused")
	@Test
	public void testMergingOriginallyNullComponent(SessionFactoryScope factoryScope) {
		// step1, we create a definition with one value, but with a null component
		factoryScope.inTransaction( (session ) -> {
			Definition definition = new Definition( 1, "stuff" );
			Value value1 = new Value( 1, definition );
			session.persist( definition );
		} );

		// step2, we verify that the definition has one value; then we detach it
		Definition detached = factoryScope.fromTransaction( (session) -> {
			Definition loaded = session.find( Definition.class, 1 );
			assertThat( loaded.getValues() ).hasSize( 1 );
			return loaded;
		} );

		// step3, we add a new value during detachment
		detached.getValues().iterator().next().getLocalizedStrings().addString( new Locale( "en_US" ), "hello" );
		Value value2 = new Value( 2, detached );
		value2.getLocalizedStrings().addString( new Locale( "es" ), "hola" );

		// step4 we merge the definition
		factoryScope.inTransaction( (session) -> {
			session.merge( detached );
		} );

		// step5, final test
		factoryScope.inTransaction( (session) -> {
			Definition definition = session.find( Definition.class, 1 );
			assertThat( definition.getValues() ).hasSize( 2 );
			for ( Value v : definition.getValues() ) {
				assertThat( v.getLocalizedStrings().makeStringsCopy() ).hasSize( 1 );
			}
		} );
	}
}
