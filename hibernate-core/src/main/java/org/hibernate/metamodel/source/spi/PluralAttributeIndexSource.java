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
package org.hibernate.metamodel.source.spi;

import java.util.List;

import org.hibernate.metamodel.internal.binder.Binder;
import org.hibernate.metamodel.spi.PluralAttributeIndexNature;

/**
 * Highly abstract concept of the index of an "indexed persistent collection".
 * More concretely (and generally more usefully) categorized as either:<ul>
 *     <li>{@link PluralAttributeSequentialIndexSource} - for list/array indexes</li>
 *     <li>{@link PluralAttributeMapKeySource} - for map keys</li>
 * </ul>
 *
 */
public interface PluralAttributeIndexSource extends RelationalValueSourceContainer {
	PluralAttributeIndexNature getNature();
	List<Binder.DefaultNamingStrategy> getDefaultNamingStrategies();
	/**
	 * Obtain information about the Hibernate index type ({@link org.hibernate.type.Type})
	 * for this plural attribute index.
	 *
	 * @return The Hibernate type information
	 */
	public HibernateTypeSource getTypeInformation();

}
