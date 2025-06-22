/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.options;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SchemaManager;
import jakarta.persistence.SchemaValidationException;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;

@Jpa(annotatedClasses = OptionsTest.WithOptions.class,
		useCollectingStatementInspector = true)
@RequiresDialect(value = PostgreSQLDialect.class, majorVersion = 14)
public class OptionsTest {
	@Test void test(EntityManagerFactoryScope scope) throws SchemaValidationException {
		SchemaManager schemaManager = scope.getEntityManagerFactory().getSchemaManager();
		schemaManager.drop(true);
		schemaManager.create(true);
		schemaManager.validate();
	}
	@Entity
	@Table(name = "TableWithOptions",
			indexes = @Index(columnList = "name", options = "nulls distinct"),
			uniqueConstraints = @UniqueConstraint(columnNames = "name", options = "deferrable"))
	static class WithOptions {
		@Id
		long id;

		@Column(name = "name", options = "compression pglz")
		String name;

		@ManyToOne
		@JoinColumn(foreignKey = @ForeignKey(name = "ToOther", options = "match full"))
		WithOptions other;
	}
}
