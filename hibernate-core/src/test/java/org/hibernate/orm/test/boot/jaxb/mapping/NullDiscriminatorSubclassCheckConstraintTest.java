/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb.mapping;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.mapping.CheckConstraint;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Table;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the subclass not-null check constraint is correct
 * when a subclass uses {@code discriminator-value="null"}.
 * <p>
 * The constraint should use "is not null" (meaning "if you're NOT this subclass,
 * skip the check") rather than "is null" (which would fail for all other subclasses).
 */
@JiraKey("HHH-20691")
public class NullDiscriminatorSubclassCheckConstraintTest {

	@Test
	public void testSubclassCheckConstraint() {
		try (var serviceRegistry = new StandardServiceRegistryBuilder()
				.applySetting( MappingSettings.TRANSFORM_HBM_XML, true )
				.build()) {
			final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( serviceRegistry )
					.addResource( "xml/jaxb/mapping/null-disc-subclass-check/hbm.xml" )
					.buildMetadata();

			final var binding = metadata.getEntityBinding(
					"org.hibernate.orm.test.boot.jaxb.mapping.NullDiscSubclassCheckBase"
			);
			assertThat( binding ).isInstanceOf( RootClass.class );

			final Table table = binding.getTable();
			for ( CheckConstraint check : table.getChecks() ) {
				final String constraint = check.getConstraint();
				assertThat( constraint )
						.as( "Subclass check constraint should not use 'disc_type is null' " +
								"which would fail for non-null discriminator rows. " +
								"Actual: " + constraint )
						.doesNotContain( "disc_type is null" );
			}
		}
	}
}
