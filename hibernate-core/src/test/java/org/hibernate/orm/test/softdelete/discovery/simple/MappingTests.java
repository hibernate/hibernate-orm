/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.softdelete.discovery.simple;

import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.orm.test.softdelete.MappingVerifier;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * Centralizes the checks about column names, values, etc.
 * to avoid problems across dialects
 *
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = {
		BooleanEntity.class,
		NumericEntity.class,
		TrueFalseEntity.class,
		YesNoEntity.class,
		ReversedYesNoEntity.class
})
@SessionFactory(exportSchema = false)
@SuppressWarnings("unused")
public class MappingTests {
	@Test
	void verifyEntityMappings(SessionFactoryScope scope) {
		final MappingMetamodelImplementor metamodel = scope.getSessionFactory().getMappingMetamodel();

		MappingVerifier.verifyMapping(
				metamodel.getEntityDescriptor( BooleanEntity.class ).getSoftDeleteMapping(),
				"deleted",
				"boolean_entity",
				true
		);

		MappingVerifier.verifyMapping(
				metamodel.getEntityDescriptor( NumericEntity.class ).getSoftDeleteMapping(),
				"deleted",
				"numeric_entity",
				1
		);

		MappingVerifier.verifyMapping(
				metamodel.getEntityDescriptor( TrueFalseEntity.class ).getSoftDeleteMapping(),
				"deleted",
				"true_false_entity",
				'T'
		);

		MappingVerifier.verifyMapping(
				metamodel.getEntityDescriptor( YesNoEntity.class ).getSoftDeleteMapping(),
				"deleted",
				"yes_no_entity",
				'Y'
		);

		MappingVerifier.verifyMapping(
				metamodel.getEntityDescriptor( ReversedYesNoEntity.class ).getSoftDeleteMapping(),
				"active",
				"reversed_yes_no_entity",
				'N'
		);
	}

}
