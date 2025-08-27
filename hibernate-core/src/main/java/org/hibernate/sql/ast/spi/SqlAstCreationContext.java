/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.type.BindingContext;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * The "context" in which creation of SQL AST occurs. Provides
 * access to stuff generally needed when creating SQL AST nodes
 * <p>
 * Because we would like to be able to render SQL during the
 * startup cycle, before the {@code SessionFactory} is completely
 * initialized, code involved in SQL AST creation and rendering
 * should avoid making use of the {@code SessionFactory}.
 * Instead, use the objects exposed by this creation context.
 *
 * @author Steve Ebersole
 */
public interface SqlAstCreationContext extends BindingContext {
	/**
	 * Avoid calling this method directly, as much as possible.
	 * SQL AST creation should not depend on the existence of
	 * a session factory, so if you need to obtain this object,
	 * there's something wrong with the design.
	 * <p>
	 * Currently this is only called when creating a
	 * {@link org.hibernate.sql.ast.tree.from.TableGroup},
	 * but we will introduce a new sort of creation context
	 * for that, probably.
	 */
	@Deprecated
	SessionFactoryImplementor getSessionFactory();

	/**
	 * The runtime {@link MappingMetamodelImplementor}
	 */
	MappingMetamodelImplementor getMappingMetamodel();

	/**
	 * When creating {@link org.hibernate.sql.results.graph.Fetch} references,
	 * defines a limit to how deep we should join for fetches.
	 */
	Integer getMaximumFetchDepth();

	/**
	 * @see org.hibernate.jpa.spi.JpaCompliance#isJpaQueryComplianceEnabled
	 */
	boolean isJpaQueryComplianceEnabled();

	/**
	 * Obtain the definition of a named {@link FetchProfile}.
	 *
	 * @param name The name of the fetch profile
	 */
	FetchProfile getFetchProfile(String name);

	/**
	 * Obtain the {@link SqmFunctionRegistry}.
	 */
	default SqmFunctionRegistry getSqmFunctionRegistry() {
		return getSessionFactory().getQueryEngine().getSqmFunctionRegistry();
	}

	/**
	 * Obtain the {@link Dialect}.
	 */
	default Dialect getDialect() {
		return getSessionFactory().getQueryEngine().getDialect();
	}

	/**
	 * Obtain the "incomplete" {@link WrapperOptions} that would be
	 * returned by {@link SessionFactoryImplementor#getWrapperOptions()}.
	 */
	default WrapperOptions getWrapperOptions() {
		return getSessionFactory().getWrapperOptions();
	}
}
