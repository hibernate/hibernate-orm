/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

@SessionFactory
@DomainModel(annotatedClasses = {IsDirtyImmutableTest.Mutable.class, IsDirtyImmutableTest.NotMutable.class})
class IsDirtyImmutableTest {
	@Test
	void test(SessionFactoryScope scope) {
		scope.inTransaction( s -> s.persist( new Mutable() ) );
		scope.inTransaction( s -> {
			var entity = s.find( NotMutable.class, 1L );
			assertFalse( s.isDirty() );
			entity.description = "new description";
			assertFalse( s.isDirty() );
		} );
	}

	@Entity
	@Table(name = "TheTable")
	static class Mutable {
		@Id
		Long id = 1L;
		String description = "old description";
	}

	@Immutable
	@Entity
	@Table(name = "TheTable")
	static class NotMutable {
		@Id
		Long id;
		String description;
	}
}
