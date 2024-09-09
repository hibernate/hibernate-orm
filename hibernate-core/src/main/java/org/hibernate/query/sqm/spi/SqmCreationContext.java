/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.spi;

import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.spi.JpaMetamodelImplementor;
import org.hibernate.query.BindingContext;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * The context in which all SQM creations occur (think SessionFactory).
 *
 * @author Steve Ebersole
 */
@Incubating
public interface SqmCreationContext extends BindingContext {
	/**
	 * Access to the domain model metadata
	 */
	JpaMetamodelImplementor getJpaMetamodel();

	/**
	 * Access to the ServiceRegistry for the context
	 */
	default ServiceRegistry getServiceRegistry() {
		return getJpaMetamodel().getServiceRegistry();
	}

	default TypeConfiguration getTypeConfiguration() {
		return getJpaMetamodel().getTypeConfiguration();
	}

	QueryEngine getQueryEngine();

	default NodeBuilder getNodeBuilder() {
		return getQueryEngine().getCriteriaBuilder();
	}
}
