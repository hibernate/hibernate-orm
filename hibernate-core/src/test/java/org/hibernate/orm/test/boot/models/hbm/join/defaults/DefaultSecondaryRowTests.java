/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.hbm.join.defaults;

import org.hibernate.mapping.Join;
import org.hibernate.mapping.PersistentClass;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A {@code <secondary-table/>} in {@code orm.xml} that declares neither
 * {@code owned} nor {@code optional} must adopt the {@code @SecondaryRow}
 * annotation defaults ({@code owned=true}, {@code optional=true}).
 *
 * @see org.hibernate.annotations.SecondaryRow
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class DefaultSecondaryRowTests {

	@Test
	@JiraKey("HHH-20891")
	@DomainModel(xmlMappings = "mappings/models/join/defaults/mapping.xml")
	void testDefaults(DomainModelScope domainModelScope) {
		final PersistentClass entityBinding =
				domainModelScope.getDomainModel().getEntityBinding( Widget.class.getName() );

		assertThat( entityBinding.getJoins() ).hasSize( 1 );
		final Join join = entityBinding.getJoins().get( 0 );
		assertThat( join.getTable().getName() ).isEqualTo( "widget_supplemental" );

		// owned=true  -> not an inverse join (Hibernate writes the row)
		assertThat( join.isInverse() ).as( "owned should default to true" ).isFalse();
		// optional=true -> outer join, row omitted when all columns null
		assertThat( join.isOptional() ).as( "optional should default to true" ).isTrue();
	}
}
