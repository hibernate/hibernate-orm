/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import jakarta.annotation.Nonnull;

import org.hibernate.boot.model.naming.spi.PhysicalNamingContext;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;

import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.mapping.ToOne;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import static org.assertj.core.api.Assertions.assertThat;

/// Verifies logical non-primary-key references under physical column naming.
///
/// @author Steve Ebersole
@BaseUnitTest
class LogicalSsnReferenceTest {
	@Test
	void logicalReferenceResolvesToPhysicalSsn() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming(
					registry, new MappingSources().addManagedClass(Person.class).addManagedClass(Passport.class),
					ImplicitNamingStrategyJpaCompliantImpl.INSTANCE, new PrefixNaming());
			((org.hibernate.boot.spi.MetadataImplementor) metadata).validate();
			var person = metadata.getEntityBinding(Person.class.getName());
			var passport = metadata.getEntityBinding(Passport.class.getName());
			assertThat(person.getProperty("socialSecurityNumber").getColumns().get(0).getName()).isEqualTo("p_ssn");
			var association = (ToOne) passport.getProperty("person").getValue();
			assertThat(association.getColumns().get(0).getName()).isEqualTo("p_person_fk");
			assertThat(association.isReferenceToPrimaryKey()).isFalse();
			assertThat(person.getReferencedProperty(association.getReferencedPropertyName()).getColumns())
					.extracting(org.hibernate.mapping.Column::getName).containsExactly("p_ssn");
			assertThat(passport.getTable().getForeignKeys()).anySatisfy(fk -> {
				assertThat(fk.getColumns()).extracting(org.hibernate.mapping.Column::getName).containsExactly("p_person_fk");
				assertThat(fk.getReferencedTable()).isSameAs(person.getTable());
				assertThat(fk.getReferencedColumns()).extracting(org.hibernate.mapping.Column::getName).containsExactly("p_ssn");
			});
		}
	}
	public static class PrefixNaming extends PhysicalNamingStrategyStandardImpl {
		@Override
		@Nonnull
		public PhysicalName toPhysicalColumnName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext env) {
			return name == null ? null : env.getPhysicalNameFactory().create("p_" + name.getText(), name.isQuoted());
		}
	}
	@Entity(name="Person")
	public static class Person {
		@Id Long id;
		@Column(name="ssn") String socialSecurityNumber;
	}
	@Entity(name="Passport")
	public static class Passport {
		@Id Long id;
		@OneToOne
		@JoinColumn(name="person_fk", referencedColumnName="ssn")
		Person person;
	}
}
