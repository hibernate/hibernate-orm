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
package org.hibernate.metamodel.source.spi;

/**
 * Represents a map key where the map key value is actually the attribute value
 * from the referenced entity.
 *
 * Only relevant to one-to-many and many-to-many associations mapped using
 * the JPA {@link javax.persistence.MapKey} annotation.
 *
 * @author Gail Badner
 * @author Steve Ebersole
 */
public interface PluralAttributeMapKeySourceEntityAttribute extends PluralAttributeIndexSource {
	/**
	 * The attribute name as reported by {@link javax.persistence.MapKey#name()}
	 *
	 * @return The attribute name; {@code null} indicates that the associated entity's
	 * id attribute should be used.
	 */
	public String getAttributeName();
}
