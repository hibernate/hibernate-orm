/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.persister.entity;

import org.hibernate.annotations.Generated;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.assertj.core.api.Assertions;

/**
 * @author Bao Ngo
 */
@SessionFactory
@DomainModel(
		annotatedClasses = InvalidMutationOperationGroupTest.GeneratedWithWritableAndNullSqlOnIdEntity.class
)
@RequiresDialect(H2Dialect.class)
public class InvalidMutationOperationGroupTest {

	@Test
	public void testGenerateStaticOperationGroupInvalidCase(SessionFactoryScope scope) {
		Assertions.assertThatThrownBy( () -> scope.inTransaction( session -> {} ))
				.getCause()
				.hasMessageStartingWith( "Column value is missing. Could not generate value for" );
	}

	@Entity
	@Table(name = "DUMMY1")
	static class GeneratedWithWritableAndNullSqlOnIdEntity {

		@Id
		@Generated(writable = true)
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

}
