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

import java.util.List;
import java.util.Map;
import javax.persistence.SharedCacheMode;

import org.jboss.jandex.IndexView;
import org.xml.sax.EntityResolver;

import org.hibernate.MultiTenancyStrategy;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.FetchProfile;
import org.hibernate.metamodel.spi.binding.IdGenerator;
import org.hibernate.metamodel.spi.binding.PluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.TypeDefinition;
import org.hibernate.type.BasicType;

/**
 * @author Steve Ebersole
 */
public interface Metadata {
	/**
	 * Exposes the options used to produce a {@link Metadata} instance.
	 */
	public static interface Options {
		public StandardServiceRegistry getServiceRegistry();

		public MetadataSourceProcessingOrder getMetadataSourceProcessingOrder();
		public NamingStrategy getNamingStrategy();
		public EntityResolver getEntityResolver();
		public SharedCacheMode getSharedCacheMode();
		public AccessType getDefaultAccessType();
		public boolean useNewIdentifierGenerators();
        public boolean isGloballyQuotedIdentifiers();
		public String getDefaultSchemaName();
		public String getDefaultCatalogName();
		public MultiTenancyStrategy getMultiTenancyStrategy();
		public IndexView getJandexView();
		public List<BasicType> getBasicTypeRegistrations();
	}

	/**
	 * Retrieve the options used to build this {@link Metadata} instance.
	 *
	 * @return The options.
	 */
	public Options getOptions();

	/**
	 * Get the builder for {@link SessionFactory} instances based on this metamodel,
	 *
	 * @return The builder for {@link SessionFactory} instances.
	 */
	public SessionFactoryBuilder getSessionFactoryBuilder();

	/**
	 * Short-hand form of building a {@link SessionFactory} through the builder without any additional
	 * option overrides.
	 *
	 * @return THe built SessionFactory.
	 */
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

	public boolean hasTypeDefinition(String name);

	public TypeDefinition getTypeDefinition(String name);

	public Iterable<TypeDefinition> getTypeDefinitions();

	public Map<String, FilterDefinition> getFilterDefinitions();

	public Iterable<NamedQueryDefinition> getNamedQueryDefinitions();

	public Iterable<NamedSQLQueryDefinition> getNamedNativeQueryDefinitions();

	public Map<String, ResultSetMappingDefinition> getResultSetMappingDefinitions();

	public Map<String,String> getImports();

	public Iterable<FetchProfile> getFetchProfiles();

	public IdGenerator getIdGenerator(String name);
}
