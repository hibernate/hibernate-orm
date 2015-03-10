/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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
package org.hibernate.boot.model.naming;

import org.hibernate.boot.model.source.spi.AttributePath;

/**
 * Context for determining the implicit name of a column used to back the key
 * of a {@link java.util.Map}.  This is used for both
 * {@link javax.persistence.MapKeyColumn} and
 * {@link javax.persistence.MapKeyJoinColumn} cases.
 *
 * @author Steve Ebersole
 *
 * @see javax.persistence.MapKeyColumn
 * @see javax.persistence.MapKeyJoinColumn
 */
public interface ImplicitMapKeyColumnNameSource extends ImplicitNameSource {
	/**
	 * Access the AttributePath for the Map attribute
	 *
	 * @return The AttributePath for the Map attribute
	 */
	public AttributePath getPluralAttributePath();
}
