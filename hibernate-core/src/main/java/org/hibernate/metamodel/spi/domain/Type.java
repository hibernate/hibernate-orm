/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.spi.domain;

import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;

/**
 * Describes a "Java type" within a domain model.  This concept builds on top of the
 * lower level {@link org.hibernate.metamodel.reflite} typing model applying more
 * O/RM semantics (like what is an entity, a composite, etc).
 *
 * @author Steve Ebersole
 */
public interface Type {
	/**
	 * Obtain the name of the type.
	 *
	 * @return The name
	 */
	public String getName();

	/**
	 * Is this attribute an aggregated composite (what JPA calls an Embeddable)?
	 *
	 * @return {@code true}, if this attribute is an aggregated composite; {@code false}, otherwise.
	 */
	public boolean isAggregate();

	/**
	 * Access the {@link org.hibernate.metamodel.reflite} descriptor corresponding to this
	 * domain model type.
	 *
	 * @return The corresponding {@link org.hibernate.metamodel.reflite} JavaTypeDescriptor
	 */
	public JavaTypeDescriptor getDescriptor();

	/**
	 * @deprecated This belongs on the attribute, not on its type.
	 */
	@Deprecated
	public boolean isAssociation();
}
