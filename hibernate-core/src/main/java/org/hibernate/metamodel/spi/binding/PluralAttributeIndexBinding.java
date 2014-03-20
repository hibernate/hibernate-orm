/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.spi.binding;

import java.util.List;

import org.hibernate.metamodel.spi.domain.Type;
import org.hibernate.metamodel.spi.relational.Value;

/**
 * @author Steve Ebersole
 */
public interface PluralAttributeIndexBinding {
	IndexedPluralAttributeBinding getIndexedPluralAttributeBinding();

	Nature getNature();

	/**
	 * Retrieve the relational aspect of the index binding. Essentially describes the
	 * column(s)/derived value(s) to which the binding maps the indexes.
	 *
	 * @return The relational values.
	 */
	public List<RelationalValueBinding> getRelationalValueBindings();

	List<Value> getValues();

	HibernateTypeDescriptor getHibernateTypeDescriptor();

	Type getPluralAttributeIndexType();

	/**
	 * Describes the nature of plural attribute indexes in terms of relational implications.
	 *
	 * @author Steve Ebersole
	 */
	enum Nature {
		/**
		 * The collection indexes are basic, simple values.  This is the only valid nature for lists
		 */
		BASIC,
		/**
		 * The map key is an aggregated composite
		 */
		AGGREGATE,
		/**
		 * The map key is an association identified by a column(s) on the collection table.
		 */
		MANY_TO_MANY,
		/**
		 * The map key is represented by a Hibernate ANY mapping
		 */
		MANY_TO_ANY
	}
}
