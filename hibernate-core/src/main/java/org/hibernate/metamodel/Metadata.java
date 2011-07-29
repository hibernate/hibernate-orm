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

import java.util.Map;
import javax.persistence.SharedCacheMode;

import org.hibernate.SessionFactory;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.FetchProfile;
import org.hibernate.metamodel.binding.IdGenerator;
import org.hibernate.metamodel.binding.PluralAttributeBinding;
import org.hibernate.metamodel.binding.TypeDef;

/**
 * @author Steve Ebersole
 */
public interface Metadata {
	/**
	 * Exposes the options used to produce a {@link Metadata} instance.
	 */
	public static interface Options {
		public MetadataSourceProcessingOrder getMetadataSourceProcessingOrder();
		public NamingStrategy getNamingStrategy();
		public SharedCacheMode getSharedCacheMode();
		public AccessType getDefaultAccessType();
		public boolean useNewIdentifierGenerators();
        public boolean isGloballyQuotedIdentifiers();
		public String getDefaultSchemaName();
		public String getDefaultCatalogName();
	}

	public Options getOptions();

	public SessionFactoryBuilder getSessionFactoryBuilder();

	public SessionFactory buildSessionFactory();

	public Iterable<EntityBinding> getEntityBindings();

	public EntityBinding getEntityBinding(String entityName);

	/**
	 * Get the "root" entity binding
	 * @param entityName
	 * @return the "root entity binding; simply returns entityBinding if it is the root entity binding
	 */
	public EntityBinding getRootEntityBinding(String entityName);

	public Iterable<PluralAttributeBinding> getCollectionBindings();

	public TypeDef getTypeDefinition(String name);

	public Iterable<TypeDef> getTypeDefinitions();

	public Iterable<FilterDefinition> getFilterDefinitions();

	public Iterable<NamedQueryDefinition> getNamedQueryDefinitions();

	public Iterable<NamedSQLQueryDefinition> getNamedNativeQueryDefinitions();

	public Iterable<ResultSetMappingDefinition> getResultSetMappingDefinitions();

	public Iterable<Map.Entry<String, String>> getImports();

	public Iterable<FetchProfile> getFetchProfiles();

	public IdGenerator getIdGenerator(String name);
}
