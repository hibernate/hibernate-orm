/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy.ejb3joincolumn;

import java.util.List;
import java.util.Locale;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.internal.AnnotatedJoinColumn;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link AnnotatedJoinColumn} and {@link org.hibernate.boot.model.naming.PhysicalNamingStrategy}
 * interaction
 *
 * @author Anton Wimmer
 * @author Steve Ebersole
 */
@BaseUnitTest
public class Tests {

	@Test
	@JiraKey(value = "HHH-9961")
	public void testJpaJoinColumnPhysicalNaming() {
		final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySettings( Environment.getProperties() )
				.build();
		try {
			final MetadataSources metadataSources = new MetadataSources( ssr );
			metadataSources.addAnnotatedClass( Language.class );

			final MetadataBuilder metadataBuilder = metadataSources.getMetadataBuilder();
			metadataBuilder.applyImplicitNamingStrategy( ImplicitNamingStrategyJpaCompliantImpl.INSTANCE );
			metadataBuilder.applyPhysicalNamingStrategy( PhysicalNamingStrategyImpl.INSTANCE );

			final Metadata metadata = metadataBuilder.build();
			( (MetadataImplementor) metadata ).orderColumns( false );
			( (MetadataImplementor) metadata ).validate();

			final PersistentClass languageBinding = metadata.getEntityBinding( Language.class.getName() );
			final Property property = languageBinding.getProperty( "fallBack" );
			List<Selectable> selectables = property.getValue().getSelectables();
			assertTrue( selectables.size() == 1 );
			final Column column = (Column) selectables.get( 0 );

			assertEquals( "C_FALLBACK_ID", column.getName().toUpperCase( Locale.ROOT ) );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}
}
