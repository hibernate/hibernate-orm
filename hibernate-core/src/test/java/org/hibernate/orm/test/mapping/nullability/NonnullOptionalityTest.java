/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.nullability;

import jakarta.annotation.Nonnull;
import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

import org.hibernate.cfg.ValidationSettings;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@ServiceRegistry(settings = @Setting(name = ValidationSettings.JAKARTA_VALIDATION_MODE, value = "none"))
@DomainModel(annotatedClasses = {
		NonnullOptionalityTest.Author.class,
		NonnullOptionalityTest.Book.class,
		NonnullOptionalityTest.Cover.class
})
@SessionFactory
class NonnullOptionalityTest {
	@Test
	void nonnullImpliesNonOptional(SessionFactoryScope scope) {
		final PersistentClass bookMapping = scope.getMetadataImplementor().getEntityBinding( Book.class.getName() );

		assertNonOptional( bookMapping, "title" );
		assertNonOptional( bookMapping, "subtitle" );
		assertNonOptional( bookMapping, "author" );
		assertNonOptional( bookMapping, "cover" );
	}

	private static void assertNonOptional(PersistentClass classMapping, String propertyName) {
		final Property property = classMapping.getProperty( propertyName );
		assertThat( property.isOptional() ).isFalse();
		assertThat( property.getColumns().get( 0 ).isNullable() ).isFalse();
	}

	@Entity(name = "NonnullOptionalityAuthor")
	public static class Author {
		@Id
		private Integer id;
	}

	@Entity(name = "NonnullOptionalityCover")
	public static class Cover {
		@Id
		private Integer id;
	}

	@Entity(name = "NonnullOptionalityBook")
	public static class Book {
		@Id
		private Integer id;

		@Nonnull
		private String title;

		@Basic(optional = true)
		@Nonnull
		private String subtitle;

		@Nonnull
		@ManyToOne
		private Author author;

		@Nonnull
		@OneToOne
		private Cover cover;
	}
}
