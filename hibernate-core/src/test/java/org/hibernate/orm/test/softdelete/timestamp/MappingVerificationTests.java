/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.softdelete.timestamp;

import org.hibernate.boot.MetadataSources;
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.orm.test.softdelete.MappingVerifier;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Avoid schema export and population for simple model verifications
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class MappingVerificationTests {
	@Test
	@DomainModel(annotatedClasses = Employee.class)
	@SessionFactory(exportSchema = false)
	void verifyModel(SessionFactoryScope scope) {
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory().getMappingMetamodel();

		final EntityPersister entityMapping = mappingMetamodel.getEntityDescriptor( Employee.class );
		MappingVerifier.verifyTimestampMapping(
				entityMapping.getSoftDeleteMapping(),
				"deleted_at",
				"employees"
		);

		final PluralAttributeMapping accoladesMapping = (PluralAttributeMapping) entityMapping.findAttributeMapping( "accolades" );
		MappingVerifier.verifyTimestampMapping(
				accoladesMapping.getSoftDeleteMapping(),
				"deleted_on",
				"employee_accolades"
		);
	}

	@Test
	void testBadEntityMapping() {
		try {
			new MetadataSources()
					.addAnnotatedClass( BadJuju.class )
					.buildMetadata();
			fail( "Expecting a failure" );
		}
		catch (UnsupportedMappingException expected) {
		}
	}

	@Test
	void testBadCollectionMapping() {
		try {
			new MetadataSources()
					.addAnnotatedClass( BadAss.class )
					.buildMetadata();
			fail( "Expecting a failure" );
		}
		catch (UnsupportedMappingException expected) {
		}
	}
}
