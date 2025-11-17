/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.softdelete.discovery.pkg2;

import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.orm.test.softdelete.MappingVerifier;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = AnEntity.class )
@SessionFactory
public class PackageLevelSoftDeleteTests2 {
	@Test
	public void verifySchema(SessionFactoryScope scope) {
		final MappingMetamodelImplementor metamodel = scope.getSessionFactory().getMappingMetamodel();
		MappingVerifier.verifyMapping(
				metamodel.getEntityDescriptor( AnEntity.class ).getSoftDeleteMapping(),
				"gone",
				"the_table",
				'T'
		);
	}
}
