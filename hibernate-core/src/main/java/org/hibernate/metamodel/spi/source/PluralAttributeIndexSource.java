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
package org.hibernate.metamodel.spi.source;

import java.util.List;

import org.hibernate.metamodel.internal.Binder;
import org.hibernate.metamodel.spi.binding.PluralAttributeIndexBinding;

/**
 *
 */
public interface PluralAttributeIndexSource extends RelationalValueSourceContainer {
	PluralAttributeIndexBinding.Nature getNature();
	List<Binder.DefaultNamingStrategy> getDefaultNamingStrategies();
	/**
	 * Obtain information about the Hibernate index type ({@link org.hibernate.type.Type})
	 * for this plural attribute index.
	 *
	 * @return The Hibernate type information
	 */
	public HibernateTypeSource getTypeInformation();

	/**
	 * Is this plural attribute index source for an attribute of the referenced entity
	 * (relevant only for one-to-many and many-to-many associations)?
	 *
	 * If this method returns {@code true}, then this object can safely
	 * be cast to EntityAttributePluralAttributeIndexSource.
	 *
	 * @return true, if this plural attribute index source for an attribute of the referenced
	 * entity; false, otherwise.
	 */
	public boolean isReferencedEntityAttribute();
}
