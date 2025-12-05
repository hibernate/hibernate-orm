/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.inheritance.hhh_x;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;


/**
 * @author Andrea Boriero
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
@ServiceRegistry
@DomainModel(annotatedClasses = {Step.class, GroupStep.class})
public class InheritanceSchemaUpdateTest {

	@Test
	public void testBidirectionalOneToManyReferencingRootEntity(DomainModelScope modelScope) {
		final var metadata = modelScope.getDomainModel();
		metadata.orderColumns( false );
		metadata.validate();

		try {
			new SchemaUpdate().execute( EnumSet.of( TargetType.DATABASE ), metadata );
		}
		finally {
			new SchemaExport().drop( EnumSet.of( TargetType.DATABASE ), metadata );
		}
	}
}
