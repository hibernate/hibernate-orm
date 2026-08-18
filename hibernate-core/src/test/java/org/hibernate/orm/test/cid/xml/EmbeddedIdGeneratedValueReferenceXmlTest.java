/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cid.xml;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that a {@code <generated-value>} declared on an {@code <embedded-id>} and
 * referencing a named {@code <generic-generator>} (by its {@code generator} attribute)
 * is applied to a composite identifier.
 */
@JiraKey("HHH-20790")
@DomainModel(xmlMappings = "org/hibernate/orm/test/cid/xml/EventGeneratedValue.orm.xml")
@SessionFactory
public class EmbeddedIdGeneratedValueReferenceXmlTest {

	@Test
	public void testGeneratedCompositeId(SessionFactoryScope scope) {
		final Event event = new Event();
		event.setName( "launch" );

		scope.inTransaction( session -> session.persist( event ) );

		// the referenced generator must have populated the composite id
		final EventId generatedId = event.getId();
		assertThat( generatedId ).isNotNull();
		assertThat( generatedId.getRegion() ).isGreaterThan( 0 );
		assertThat( generatedId.getSequence() ).isGreaterThan( 0 );

		scope.inTransaction( session -> {
			final Event found = session.get( Event.class, generatedId );
			assertThat( found ).isNotNull();
			assertThat( found.getName() ).isEqualTo( "launch" );
			assertThat( found.getId() ).isEqualTo( generatedId );
		} );
	}
}
