/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.write.staticinsert;

import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
public class SimpleNullabilityTest {
	@Test
	@DomainModel(annotatedClasses = Tester.class)
	void checkIt(DomainModelScope scope) {
		final PersistentClass entityBinding = scope.getEntityBinding( Tester.class );
		final Property descriptionProperty = entityBinding.getProperty( "description" );
		assertThat( descriptionProperty.isOptional() ).isTrue();
		assertThat( descriptionProperty.getColumns().get( 0 ).isNullable() ).isTrue();
	}

	@Entity(name="Tester")
	@Table(name="Tester")
	@SecondaryTable(name="Tester2")
	public static class Tester {
		@Id
		private Integer id;
		private String name;
		@Column(table = "Tester2")
		private String description;
	}
}
