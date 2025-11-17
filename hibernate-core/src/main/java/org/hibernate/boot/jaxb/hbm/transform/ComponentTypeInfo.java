/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.hbm.transform;

import org.hibernate.mapping.Component;

/**
 * @author Steve Ebersole
 */
public class ComponentTypeInfo extends ManagedTypeInfo {
	private final Component component;

	public ComponentTypeInfo(Component component) {
		super( component.getTable() );
		this.component = component;
	}

	public Component getComponent() {
		return component;
	}
}
