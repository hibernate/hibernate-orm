package org.hibernate.action.queue;

import org.hibernate.action.queue.constraint.ConstraintModel;
import org.hibernate.action.queue.constraint.ConstraintModelBuilder;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;

import static org.hibernate.cfg.FlushSettings.ORDER_BY_FOREIGN_KEY;
import static org.hibernate.cfg.FlushSettings.ORDER_BY_UNIQUE_KEY;
import static org.hibernate.engine.config.spi.StandardConverters.BOOLEAN;

/// ActionQueueFactory for building GraphBasedActionQueue instances.
///
/// @author Steve Ebersole
public class GraphBasedActionQueueFactory implements ActionQueueFactory {
	private final PlanningOptions planningOptions;
	private final ConstraintModel constraintModel;

	public GraphBasedActionQueueFactory(SessionFactoryImplementor factory) {
		var configurationService = factory.getServiceRegistry().requireService( ConfigurationService.class );
		planningOptions = buildPlanningOptions( configurationService );
		constraintModel = buildConstraintModel( factory, planningOptions );
	}

	private static PlanningOptions buildPlanningOptions(ConfigurationService configurationService) {
		var orderByFk = configurationService.getSetting( ORDER_BY_FOREIGN_KEY, BOOLEAN, true );
		var orderByUnique = configurationService.getSetting( ORDER_BY_UNIQUE_KEY, BOOLEAN, true );

		return new PlanningOptions( orderByFk, orderByUnique, PlanningOptions.UniqueCycleStrategy.IGNORE_UNIQUE_EDGES_IN_CYCLES );
	}

	private static ConstraintModel buildConstraintModel(SessionFactoryImplementor factory, PlanningOptions planningOptions) {
		// todo : account for PlanningOptions - do not build UniqueConstraints if we will not be using them
		return new ConstraintModelBuilder().build( factory.getMappingMetamodel() );
	}

	@Override
	public ActionQueue buildActionQueue(SessionImplementor session) {
		return new GraphBasedActionQueue( constraintModel, planningOptions, session );
	}
}
