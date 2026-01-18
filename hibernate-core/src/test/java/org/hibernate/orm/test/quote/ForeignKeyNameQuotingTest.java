/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.quote;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hibernate.cfg.MappingSettings.GLOBALLY_QUOTED_IDENTIFIERS;

@Jpa(annotatedClasses = ForeignKeyNameQuotingTest.Self.class, exportSchema = false,
		integrationSettings = @Setting(name = GLOBALLY_QUOTED_IDENTIFIERS, value = "true"))
class ForeignKeyNameQuotingTest {
	@Test void test(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().create( true );
	}
	@Entity
	@Table(uniqueConstraints = @UniqueConstraint(columnNames = "myself", name = "myuniqueself" ),
		indexes = @Index(columnList = "uuid,self", name = "myindex"),
		check = @CheckConstraint(name="checkmyself", constraint = "myself not null"))
	static class Self {
		@Id @GeneratedValue
		UUID uuid;
		@ManyToOne
		@JoinColumn(name = "myself",
				foreignKey = @ForeignKey(name = "myownself"))
		Self self;
	}
}
