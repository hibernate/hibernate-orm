/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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

/**
 * Describes an attribute.
 *
 * @author Steve Ebersole
 */
public interface Attribute {
	/**
	 * Retrieve the attribute name.
	 *
	 * @return The attribute name.
	 */
	public String getName();

	/**
	 * Retrieve the declaring container for this attribute (entity or aggregated composite).
	 *
	 * @return The attribute container.
	 */
	public AttributeContainer getAttributeContainer();

	/**
	 * An attribute can be either:<ul>
	 * <li>singular - castable to {@link SingularAttribute}</li>
	 * <li>plural - castable to {@link PluralAttribute}
	 * </ul>
	 *
	 * @return True if attribute is singular; false if plural.
	 */
	public boolean isSingular();

	/**
	 * Synthetic attributes do not really exist in the users domain classes.  Hibernate sometimes generates these
	 * synthetic attributes for various reasons.  Some parts of the code base use the phrase "virtual" as well.
	 *
	 * @return {@code true} indicates this attribute is synthetic; {@code false} indicates it is non-synthetic
	 * (an actual attribute).
	 */
	public boolean isSynthetic();
}
