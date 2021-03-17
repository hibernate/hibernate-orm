/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.lang.reflect.Member;
import java.lang.reflect.Modifier;

/**
 * Acts as a virtual Member definition for dynamic (Map-based) models.
 *
 * @author Brad Koehn
 */
public class MapMember implements Member {
	private String name;
	private final Class<?> type;

	public MapMember(String name, Class<?> type) {
		this.name = name;
		this.type = type;
	}

	public Class<?> getType() {
		return type;
	}

	public int getModifiers() {
		return Modifier.PUBLIC;
	}

	public boolean isSynthetic() {
		return false;
	}

	public String getName() {
		return name;
	}

	public Class<?> getDeclaringClass() {
		return null;
	}
}
