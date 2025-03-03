/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.softdelete.collections;

import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.orm.test.softdelete.MappingVerifier;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = { CollectionOwner.class, CollectionOwned.class })
@SessionFactory(exportSchema = false)
@SuppressWarnings({"unused", "JUnitMalformedDeclaration"})
public class MappingTests {

	@Test
	void verifyCollectionMappings(SessionFactoryScope scope) {
		final MappingMetamodelImplementor metamodel = scope.getSessionFactory().getMappingMetamodel();

		MappingVerifier.verifyMapping(
				metamodel.getCollectionDescriptor( CollectionOwner.class.getName() + ".elements" ).getAttributeMapping().getSoftDeleteMapping(),
				"deleted",
				"elements",
				'Y'
		);

		MappingVerifier.verifyMapping(
				metamodel.getCollectionDescriptor( CollectionOwner.class.getName() + ".manyToMany" ).getAttributeMapping().getSoftDeleteMapping(),
				"gone",
				"m2m",
				1
		);
	}

}
