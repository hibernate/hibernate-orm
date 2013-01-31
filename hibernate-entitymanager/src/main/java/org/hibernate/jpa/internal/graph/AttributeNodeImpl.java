/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.internal.graph;

import javax.persistence.AttributeNode;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Type;

/**
 * Hibernate implementation of the JPA AttributeNode contract
 *
 * @author Steve Ebersole
 */
public class AttributeNodeImpl<T> implements AttributeNode<T>, GraphNode<T> {
	private final Attribute attribute;

	public AttributeNodeImpl(Attribute attribute) {
		this.attribute = attribute;
	}

	@Override
	public AttributeNodeImpl<T> makeImmutableCopy() {
		// simple attribute nodes are immutable already
		return this;
	}

	@Override
	public Type<T> getType() {
		// Linda says this is going away
		return null;
	}

	@Override
	public String getAttributeName() {
		return attribute.getName();
	}

	public Attribute getAttribute() {
		return attribute;
	}

	@Override
	public String getRegistrationName() {
		return getAttributeName();
	}
}
