/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cascade.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.cascade.spi.CascadePoint;
import org.hibernate.cascade.spi.CascadeStyle;
import org.hibernate.cascade.spi.CascadingAction;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;

/// Mutable state scoped to one entity-rooted cascade traversal.
///
/// The context deliberately does not track visited entities. Action-specific
/// contexts remain responsible for cycle and duplicate handling across nested
/// event-driven cascade invocations.
///
/// @param <T> The action-specific context type
///
/// @author Steve Ebersole
final class CascadeTraversalContext<T> {
	private final CascadingAction<T> action;
	private final EventSource session;
	private final EntityPersister rootPersister;
	private final Object root;
	private final T actionContext;
	private final CascadeDecisionTrace decisionTrace;
	private final CascadeEffectMode effectMode;

	private CascadePoint cascadePoint;
	private Object currentParent;
	private String currentPropertyName;
	private Type currentPropertyType;
	private CascadeStyle currentCascadeStyle;
	private int currentPropertyIndex = -1;
	private ArrayList<String> path;
	private Throwable recordedFailure;
	private List<String> failurePath;

	CascadeTraversalContext(
			CascadingAction<T> action,
			CascadePoint cascadePoint,
			EventSource session,
			EntityPersister rootPersister,
			Object root,
			T actionContext) {
		this( action, cascadePoint, session, rootPersister, root, actionContext, null, CascadeEffectMode.EXECUTE );
	}

	CascadeTraversalContext(
			CascadingAction<T> action,
			CascadePoint cascadePoint,
			EventSource session,
			EntityPersister rootPersister,
			Object root,
			T actionContext,
			CascadeDecisionTrace decisionTrace) {
		this(
				action,
				cascadePoint,
				session,
				rootPersister,
				root,
				actionContext,
				decisionTrace,
				CascadeEffectMode.EXECUTE
		);
	}

	CascadeTraversalContext(
			CascadingAction<T> action,
			CascadePoint cascadePoint,
			EventSource session,
			EntityPersister rootPersister,
			Object root,
			T actionContext,
			CascadeDecisionTrace decisionTrace,
			CascadeEffectMode effectMode) {
		this.action = action;
		this.cascadePoint = cascadePoint;
		this.session = session;
		this.rootPersister = rootPersister;
		this.root = root;
		this.actionContext = actionContext;
		this.decisionTrace = decisionTrace;
		this.effectMode = effectMode;
		currentParent = root;
	}

	CascadingAction<T> action() {
		return action;
	}

	CascadePoint cascadePoint() {
		return cascadePoint;
	}

	CascadePoint changeCascadePoint(CascadePoint cascadePoint) {
		final var previous = this.cascadePoint;
		this.cascadePoint = cascadePoint;
		return previous;
	}

	EventSource session() {
		return session;
	}

	EntityPersister rootPersister() {
		return rootPersister;
	}

	String rootEntityName() {
		return rootPersister.getEntityName();
	}

	Object root() {
		return root;
	}

	T actionContext() {
		return actionContext;
	}

	boolean executeEffects() {
		return effectMode == CascadeEffectMode.EXECUTE;
	}

	boolean traceEnabled() {
		return decisionTrace != null;
	}

	void trace(CascadeTraceEvent event) {
		decisionTrace.record( event );
	}

	CascadeTraceEvent.Location rootTraceLocation() {
		return new CascadeTraceEvent.Location(
				rootEntityName(),
				List.of(),
				CascadeTraceEvent.NodeKind.ROOT,
				"",
				action.toString(),
				cascadePoint
		);
	}

	CascadeTraceEvent.Location traceLocation(Type type, CascadeStyle style, List<String> propertyPath) {
		return new CascadeTraceEvent.Location(
				rootEntityName(),
				propertyPath,
				CascadeTraceEvent.NodeKind.from( type ),
				style.toString(),
				action.toString(),
				cascadePoint
		);
	}

	CascadeTraceEvent.Location currentTraceLocation() {
		return traceLocation( currentPropertyType, currentCascadeStyle, currentPropertyPath() );
	}

	CascadeTraceEvent.Location currentPathTraceLocation(Type type, CascadeStyle style) {
		return traceLocation( type, style, currentPropertyPath() );
	}

	CascadeTraceEvent.Location childTraceLocation(Type type, CascadeStyle style, String propertyName) {
		if ( path == null ) {
			return traceLocation( type, style, List.of( propertyName ) );
		}
		final var childPath = new ArrayList<String>( path.size() + 1 );
		childPath.addAll( path );
		childPath.add( propertyName );
		return traceLocation( type, style, childPath );
	}

	void traceFailure(Throwable failure) {
		if ( recordedFailure != failure ) {
			recordedFailure = failure;
			failurePath = currentPropertyType == null ? List.of() : currentPropertyPath();
			if ( decisionTrace != null ) {
				trace( new CascadeTraceEvent.Failure(
						currentPropertyType == null ? rootTraceLocation() : currentTraceLocation(),
						failure.getClass().getName(),
						failure.getMessage()
				) );
			}
		}
	}

	void attachFailurePath(Throwable failure) {
		traceFailure( failure );
		for ( Throwable suppressed : failure.getSuppressed() ) {
			if ( suppressed instanceof CascadePathDiagnostic ) {
				return;
			}
		}
		failure.addSuppressed( new CascadePathDiagnostic( rootEntityName(), failurePath ) );
	}

	Object currentParent() {
		return currentParent;
	}

	String currentPropertyName() {
		return currentPropertyName;
	}

	Type currentPropertyType() {
		return currentPropertyType;
	}

	CascadeStyle currentCascadeStyle() {
		return currentCascadeStyle;
	}

	int currentPropertyIndex() {
		return currentPropertyIndex;
	}

	void setCurrentProperty(
			Object parent,
			String propertyName,
			Type propertyType,
			CascadeStyle cascadeStyle,
			int propertyIndex) {
		currentParent = parent;
		currentPropertyName = propertyName;
		currentPropertyType = propertyType;
		currentCascadeStyle = cascadeStyle;
		currentPropertyIndex = propertyIndex;
	}

	List<String> path() {
		return path;
	}

	private List<String> currentPropertyPath() {
		if ( path == null ) {
			return currentPropertyName == null ? List.of() : List.of( currentPropertyName );
		}
		final var result = new ArrayList<String>( path.size() + 1 );
		result.addAll( path );
		if ( currentPropertyName != null ) {
			result.add( currentPropertyName );
		}
		return result;
	}

	void pushPath(String propertyName) {
		if ( path == null ) {
			path = new ArrayList<>();
			if ( decisionTrace != null ) {
				decisionTrace.pathAllocated();
			}
		}
		path.add( propertyName );
	}

	void popPath() {
		final int last = path.size() - 1;
		path.remove( last );
		if ( last == 0 ) {
			path = null;
		}
	}
}
