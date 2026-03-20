/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.support;

import org.hibernate.action.queue.ActionQueue;
import org.hibernate.action.queue.ActionQueueFactory;
import org.hibernate.action.queue.GraphBasedActionQueue;
import org.hibernate.action.queue.PlanningOptions;
import org.hibernate.action.queue.constraint.ConstraintModel;
import org.hibernate.action.queue.constraint.ConstraintModelBuilder;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;

import java.io.Serializable;

import static org.hibernate.cfg.FlushSettings.DEFERRABLE_AVOID_BREAK;
import static org.hibernate.cfg.FlushSettings.DEFERRABLE_EDGES_IGNORE;
import static org.hibernate.cfg.FlushSettings.ORDER_BY_FOREIGN_KEY;
import static org.hibernate.cfg.FlushSettings.ORDER_BY_UNIQUE_KEY;
import static org.hibernate.engine.config.spi.StandardConverters.BOOLEAN;

/// ActionQueueFactory for building GraphBasedActionQueue instances.
///
/// @author Steve Ebersole
public class GraphBasedActionQueueFactory implements ActionQueueFactory, Serializable {
	private final PlanningOptions planningOptions;
	private final ConstraintModel constraintModel;

	public GraphBasedActionQueueFactory(SessionFactoryImplementor factory) {
		var configurationService = factory.getServiceRegistry().requireService( ConfigurationService.class );
		planningOptions = buildPlanningOptions( configurationService );
		constraintModel = buildConstraintModel( factory, planningOptions );
	}

	@Override
	public ActionQueue buildActionQueue(SessionImplementor session) {
		return new GraphBasedActionQueue( constraintModel, planningOptions, session );
	}

	private static PlanningOptions buildPlanningOptions(ConfigurationService configurationService) {
		var orderByFk = configurationService.getSetting( ORDER_BY_FOREIGN_KEY, BOOLEAN, true );
		var orderByUnique = configurationService.getSetting( ORDER_BY_UNIQUE_KEY, BOOLEAN, false );

		var avoidBreakingDeferrable = configurationService.getSetting( DEFERRABLE_AVOID_BREAK, BOOLEAN, true );
		var ignoreDeferrableEdges = configurationService.getSetting( DEFERRABLE_EDGES_IGNORE, BOOLEAN, true );

		return new PlanningOptions(
				orderByFk,
				orderByUnique,
				avoidBreakingDeferrable,
				ignoreDeferrableEdges,
				PlanningOptions.UniqueCycleStrategy.IGNORE_UNIQUE_EDGES_IN_CYCLES
		);
	}

	private static ConstraintModel buildConstraintModel(SessionFactoryImplementor factory, PlanningOptions planningOptions) {
		// todo : account for PlanningOptions - do not build UniqueConstraints if we will not be using them
		return ConstraintModelBuilder.buildConstraintModel( factory.getMappingMetamodel(), planningOptions );
	}
}
