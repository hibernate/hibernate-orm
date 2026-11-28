/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable.elementcollection;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * An embeddable ({@code Revision} with a {@code value} attribute) used through an XML
 * {@code <element-collection>} ignores the {@code <attribute-override name="value">} column, so the
 * generated SQL uses the default embeddable column {@code value} instead of the overridden
 * {@code status}. Related: HHH-20324, which fixed the same {@code {element}} placeholder
 * resolution for an association-override join column but not for a plain column override.
 */
@DomainModel(xmlMappings = "org/hibernate/orm/test/mapping/embeddable/elementcollection/embeddable-element-collection-override.xml")
@SessionFactory(useCollectingStatementInspector = true)
@JiraKey(value = "HHH-20889")
public class EmbeddableElementCollectionOverrideTest {

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void testPersistAndReadUsesOverriddenColumn(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();

		scope.inTransaction( session -> {
			final Owner owner = new Owner( 1L );
			owner.getTrace().add( new Revision( "created" ) );
			session.persist( owner );
		} );

		// The @ElementCollection insert must target the overridden column "status", not the
		// default embeddable attribute column "value".
		assertThat( inspector.getSqlQueries() )
				.filteredOn( sql -> sql.toLowerCase().contains( "owner_status" ) )
				.singleElement()
				.satisfies( sql -> assertThat( sql.toLowerCase() )
						.contains( "(owner_id,status)" ) );

		scope.inTransaction( session -> assertThat(
				session.find( Owner.class, 1L ).getTrace() )
				.singleElement()
				.extracting( Revision::getValue )
				.isEqualTo( "created" ) );
	}
}
