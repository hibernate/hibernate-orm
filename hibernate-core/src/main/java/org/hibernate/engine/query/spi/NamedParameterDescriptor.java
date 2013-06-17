/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.engine.query.spi;

import java.io.Serializable;

import org.hibernate.type.Type;

/**
 * Descriptor regarding a named parameter.
 *
 * @author Steve Ebersole
 */
public class NamedParameterDescriptor implements Serializable {
	private final String name;
	private Type expectedType;
	private final int[] sourceLocations;
	private final boolean jpaStyle;

	/**
	 * Constructs a NamedParameterDescriptor
	 *
	 * @param name The name of the parameter
	 * @param expectedType The expected type of the parameter, according to the translator
	 * @param sourceLocations The locations of the named parameters (aye aye aye)
	 * @param jpaStyle Was the parameter a JPA style "named parameter"?
	 */
	public NamedParameterDescriptor(String name, Type expectedType, int[] sourceLocations, boolean jpaStyle) {
		this.name = name;
		this.expectedType = expectedType;
		this.sourceLocations = sourceLocations;
		this.jpaStyle = jpaStyle;
	}

	public String getName() {
		return name;
	}

	public Type getExpectedType() {
		return expectedType;
	}

	public int[] getSourceLocations() {
		return sourceLocations;
	}

	public boolean isJpaStyle() {
		return jpaStyle;
	}

	/**
	 * Set the parameters expected type
	 *
	 * @param type The new expected type
	 */
	public void resetExpectedType(Type type) {
		this.expectedType = type;
	}
}
