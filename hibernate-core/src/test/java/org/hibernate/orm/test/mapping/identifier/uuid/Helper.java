/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.identifier.uuid;

import org.hibernate.generator.Generator;
import org.hibernate.id.uuid.UuidGenerator;
import org.hibernate.id.uuid.UuidValueGenerator;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;

import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.util.uuid.IdGeneratorCreationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
public class Helper {
	public static void verifyAlgorithm(
			ServiceRegistryScope registryScope,
			DomainModelScope domainModelScope,
			RootClass descriptor,
			Class<? extends UuidValueGenerator> expectedAlgorithm) {
		final Property idProperty = descriptor.getIdentifierProperty();
		final BasicValue value = (BasicValue) idProperty.getValue();

		assertThat( value.getCustomIdGeneratorCreator() ).isNotNull();
		final Generator generator = value.getCustomIdGeneratorCreator().createGenerator( new IdGeneratorCreationContext(
				registryScope.getRegistry(),
				domainModelScope.getDomainModel(),
				descriptor
		));

		assertThat( generator ).isInstanceOf( UuidGenerator.class );
		final UuidGenerator uuidGenerator = (UuidGenerator) generator;
		assertThat( uuidGenerator.getValueGenerator() ).isInstanceOf( expectedAlgorithm );
	}
}
