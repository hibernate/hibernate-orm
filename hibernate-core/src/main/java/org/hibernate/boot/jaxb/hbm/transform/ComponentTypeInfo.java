/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
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
