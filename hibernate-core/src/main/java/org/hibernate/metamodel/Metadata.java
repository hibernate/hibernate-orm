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

package org.hibernate.metamodel;

import javax.persistence.SharedCacheMode;

import org.hibernate.SessionFactory;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.IdGenerator;
import org.hibernate.metamodel.binding.TypeDef;

/**
 * @author Steve Ebersole
 */
public interface Metadata {
	/**
	 * Exposes the options used to produce a {@link Metadata} instance.
	 */
	public static interface Options {
		public SourceProcessingOrder getSourceProcessingOrder();
		public NamingStrategy getNamingStrategy();
		public SharedCacheMode getSharedCacheMode();
		public AccessType getDefaultAccessType();
		public boolean useNewIdentifierGenerators();
		public String getDefaultSchemaName();
		public String getDefaultCatalogName();
	}

	public Options getOptions();

	public SessionFactory buildSessionFactory();

	public Iterable<EntityBinding> getEntityBindings();

	public EntityBinding getEntityBinding(String entityName);

	public Iterable<TypeDef> getTypeDefinitions();

	public Iterable<FilterDefinition> getFilterDefinitions();

	public IdGenerator getIdGenerator(String name);
}
