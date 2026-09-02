/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.service.ServiceRegistry;

/// Registers the external fixture's paired Java and JDBC descriptors.
///
/// @author Steve Ebersole
public final class ExampleTypeContributor implements TypeContributor {
	@Override
	public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		typeContributions.contributeJavaType( ExampleJavaType.INSTANCE );
		typeContributions.contributeJdbcType( ExampleJdbcType.INSTANCE );
		typeContributions.contributeJdbcTypeConstructor( ExampleJdbcTypeConstructor.INSTANCE );
	}
}
