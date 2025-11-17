/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.component.cascading.collection;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/component/cascading/collection/Mappings.hbm.xml"
)
@SessionFactory
public class CascadeToComponentCollectionTest {

	@Test
	public void testMerging(SessionFactoryScope scope) {
		// step1, we create a definition with one value
		Definition d = new Definition();

		scope.inTransaction( session -> {
					Value value1 = new Value( d );
					value1.getLocalizedStrings().addString( new Locale( "en_US" ), "hello" );
					session.persist( d );
				}
		);

		// step2, we verify that the definition has one value; then we detach it
		Definition def = scope.fromTransaction( session -> {
					Definition definition = session.find( Definition.class, d.getId() );
					assertThat( definition.getValues() ).hasSize( 1 );
					return definition;
				}
		);

		// step3, we add a new value during detachment
		Value value2 = new Value( def );
		value2.getLocalizedStrings().addString( new Locale( "es" ), "hola" );

		// step4 we merge the definition
		scope.inTransaction( session ->
				session.merge( def )
		);

		// step5, final test
		scope.inTransaction( session -> {
					Definition definition = session.find( Definition.class, d.getId() );
					assertThat( definition.getValues() ).hasSize( 2 );
					for ( Value o : definition.getValues() ) {
						assertThat( o.getLocalizedStrings().getStringsCopy() ).hasSize( 1 );
					}
				}
		);
	}

	@SuppressWarnings("unused")
	@Test
	public void testMergingOriginallyNullComponent(SessionFactoryScope scope) {
		// step1, we create a definition with one value, but with a null component
		Definition d = new Definition();
		scope.inTransaction( session -> {
					Value value1 = new Value( d );
					session.persist( d );
				}
		);

		// step2, we verify that the definition has one value; then we detach it
		Definition def = scope.fromTransaction( session -> {
					Definition definition = session.find( Definition.class, d.getId() );
					assertThat( definition.getValues() ).hasSize( 1 );
					return definition;
				}
		);

		// step3, we add a new value during detachment
		def.getValues().iterator().next().getLocalizedStrings()
				.addString( new Locale( "en_US" ), "hello" );
		Value value2 = new Value( def );
		value2.getLocalizedStrings().addString( new Locale( "es" ), "hola" );

		// step4 we merge the definition
		scope.inTransaction( session ->
				session.merge( def )
		);

		// step5, final test
		scope.inTransaction(
				session -> {
					Definition definition = session.find( Definition.class, def.getId() );
					assertThat( definition.getValues().size() ).isEqualTo( 2 );
					for ( Value o : definition.getValues() ) {
						assertThat( o.getLocalizedStrings().getStringsCopy() ).hasSize( 1 );
					}
				}
		);
	}
}
