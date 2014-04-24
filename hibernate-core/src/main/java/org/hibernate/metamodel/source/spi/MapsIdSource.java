/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.spi;

/**
 * Describes a relationship annotated with {@link javax.persistence.MapsId}
 * 
 * @author Steve Ebersole
 */
public interface MapsIdSource {
	/**
	 * Obtain the {@link javax.persistence.MapsId#value()} naming the attribute
	 * within the EmbeddedId mapped by this relationship.
	 * 
	 * @return The corresponding id attribute name.
	 */
	public String getMappedIdAttributeName();

	/**
	 * The attribute source information
	 * 
	 * @return The association attribute information
	 */
	public ToOneAttributeSource getAssociationAttributeSource();
}
