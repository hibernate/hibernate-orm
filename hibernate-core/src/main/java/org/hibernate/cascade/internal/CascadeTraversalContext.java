/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cascade.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.cascade.spi.CascadeStyle;
import org.hibernate.cascade.spi.CascadingAction;
import org.hibernate.cascade.spi.CascadePoint;
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

	private CascadePoint cascadePoint;
	private Object currentParent;
	private String currentPropertyName;
	private Type currentPropertyType;
	private CascadeStyle currentCascadeStyle;
	private int currentPropertyIndex = -1;
	private ArrayList<String> path;

	CascadeTraversalContext(
			CascadingAction<T> action,
			CascadePoint cascadePoint,
			EventSource session,
			EntityPersister rootPersister,
			Object root,
			T actionContext) {
		this.action = action;
		this.cascadePoint = cascadePoint;
		this.session = session;
		this.rootPersister = rootPersister;
		this.root = root;
		this.actionContext = actionContext;
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

	void pushPath(String propertyName) {
		if ( path == null ) {
			path = new ArrayList<>();
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
