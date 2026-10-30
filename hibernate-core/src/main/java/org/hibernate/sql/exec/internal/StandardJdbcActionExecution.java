package org.hibernate.sql.exec.internal;

import java.sql.Connection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Arrays;

import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcActionExecution;
import org.hibernate.sql.exec.spi.LoadedValuesCollector;
import org.hibernate.sql.exec.spi.PostAction;
import org.hibernate.sql.exec.spi.PreAction;
import org.hibernate.sql.exec.spi.SecondaryAction;
import org.hibernate.sql.exec.spi.StatementAccess;

/// Binds reusable definitions to one invocation and tracks post-action progress.
///
/// @author Steve Ebersole
public final class StandardJdbcActionExecution implements JdbcActionExecution {
	private final List<PreAction> preActions;
	private final List<PostAction> postActions;
	private boolean preActionsStarted;
	private boolean postActionsStarted;
	private int nextPostAction;

	private StandardJdbcActionExecution(List<PreAction> preActions, List<PostAction> postActions) {
		this.preActions = preActions;
		this.postActions = postActions;
	}

	public static JdbcActionExecution create(PreAction[] preDefinitions, PostAction[] postDefinitions) {
		if ( (preDefinitions == null || preDefinitions.length == 0)
				&& (postDefinitions == null || postDefinitions.length == 0) ) {
			return NONE;
		}
		final var instances = materialize( preDefinitions, postDefinitions );
		return new StandardJdbcActionExecution( instances.preActions(), instances.postActions() );
	}

	public static ActionInstances materialize(PreAction[] preDefinitions, PostAction[] postDefinitions) {
		final int preCount = preDefinitions == null ? 0 : preDefinitions.length;
		final int postCount = postDefinitions == null ? 0 : postDefinitions.length;
		if ( preCount == 0 && postCount == 0 ) {
			return new ActionInstances( List.of(), List.of() );
		}
		final Map<SecondaryAction, SecondaryAction> instances = new IdentityHashMap<>();
		final Set<SecondaryAction> phaseDefinitions = Collections.newSetFromMap( new IdentityHashMap<>() );
		final var preActions = new PreAction[preCount];
		for ( int i = 0; i < preCount; i++ ) {
			preActions[i] = materialize( preDefinitions[i], PreAction.class, phaseDefinitions, instances );
		}
		phaseDefinitions.clear();
		final var postActions = new PostAction[postCount];
		for ( int i = 0; i < postCount; i++ ) {
			postActions[i] = materialize( postDefinitions[i], PostAction.class, phaseDefinitions, instances );
		}
		return new ActionInstances( Arrays.asList( preActions ), Arrays.asList( postActions ) );
	}

	private static <T extends SecondaryAction> T materialize(
			SecondaryAction definition, Class<T> role, Set<SecondaryAction> phaseDefinitions,
			Map<SecondaryAction, SecondaryAction> instances) {
		if ( definition == null || !phaseDefinitions.add( definition ) ) {
			throw new IllegalArgumentException( "Null or duplicate action definition in " + role.getSimpleName() );
		}
		SecondaryAction instance = instances.get( definition );
		if ( instance == null ) {
			instance = definition.createExecutionAction();
			if ( instance == null ) {
				throw new IllegalArgumentException( "Action definition returned a null execution action" );
			}
			instances.put( definition, instance );
		}
		if ( !role.isInstance( instance ) ) {
			throw new IllegalArgumentException( "Execution action does not implement " + role.getSimpleName() );
		}
		return role.cast( instance );
	}

	@Override
	public void performPreActions(StatementAccess access, Connection connection, ExecutionContext context) {
		if ( preActionsStarted || postActionsStarted ) {
			throw new IllegalStateException( "Action execution is single-use" );
		}
		preActionsStarted = true;
		for ( var action : preActions ) {
			action.performPreAction( access, connection, context );
		}
	}

	@Override
	public void performPostActions(
			boolean succeeded, StatementAccess access, Connection connection,
			ExecutionContext context, LoadedValuesCollector collector) {
		final Throwable failure = finish( succeeded, null, access, connection, context, collector );
		if ( failure instanceof RuntimeException runtimeException ) {
			throw runtimeException;
		}
		else if ( failure instanceof Error error ) {
			throw error;
		}
	}

	@Override
	public void cleanupAfterFailure(
			Throwable failure, StatementAccess access, Connection connection,
			ExecutionContext context, LoadedValuesCollector collector) {
		finish( false, failure, access, connection, context, collector );
	}

	private Throwable finish(
			boolean succeeded, Throwable failure, StatementAccess access, Connection connection,
			ExecutionContext context, LoadedValuesCollector collector) {
		postActionsStarted = true;
		while ( nextPostAction < postActions.size() ) {
			// Advance before eligibility/callback evaluation so a failure cannot replay this slot.
			final var action = postActions.get( nextPostAction++ );
			try {
				if ( (succeeded && failure == null) || action.shouldRunAfterFail() ) {
					action.performPostAction( access, connection, context, collector );
				}
			}
			catch (RuntimeException | Error actionFailure) {
				if ( failure == null ) {
					failure = actionFailure;
				}
				else if ( failure != actionFailure ) {
					failure.addSuppressed( actionFailure );
				}
			}
		}
		return failure;
	}
}
