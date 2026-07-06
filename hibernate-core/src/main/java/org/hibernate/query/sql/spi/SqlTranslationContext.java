/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sql.spi;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.spi.SqmCreationContext;
import org.hibernate.type.BindingContext;
import org.hibernate.type.descriptor.WrapperOptions;

/// Runtime context for SQL AST creation and translation.
///
/// @since 9.0
/// @author Steve Ebersole
public interface SqlTranslationContext extends BindingContext {
	/// Obtain the context used for SQM creation/type checking.
	SqmCreationContext getSqmCreationContext();

	/// The runtime [MappingMetamodelImplementor].
	MappingMetamodelImplementor getMappingMetamodel();

	/// When creating [org.hibernate.sql.results.graph.Fetch] references,
	/// defines a limit to how deep we should join for fetches.
	Integer getMaximumFetchDepth();

	/// @see org.hibernate.jpa.spi.JpaCompliance#isJpaQueryComplianceEnabled
	boolean isJpaQueryComplianceEnabled();

	/// Obtain the definition of a named [FetchProfile].
	///
	/// @param name The name of the fetch profile
	FetchProfile getFetchProfile(String name);

	/// Obtain the [SqmFunctionRegistry].
	SqmFunctionRegistry getSqmFunctionRegistry();

	/// Obtain the [Dialect].
	Dialect getDialect();

	/// Obtain the [WrapperOptions] used while creating SQL AST nodes.
	WrapperOptions getWrapperOptions();
}
