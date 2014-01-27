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
import org.hibernate.cfg.annotations.NamedEntityGraphDefinition;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.FetchProfile;
import org.hibernate.metamodel.spi.binding.IdentifierGeneratorDefinition;
import org.hibernate.metamodel.spi.binding.PluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.SecondaryTable;
import org.hibernate.metamodel.spi.binding.TypeDefinition;
import org.hibernate.metamodel.spi.relational.Identifier;
import org.hibernate.type.BasicType;

/**
 * @author Steve Ebersole
 */
public interface Metadata {
	/**
	 * Exposes the options used to produce a {@link Metadata} instance.
	 */
	public static interface Options {
		StandardServiceRegistry getServiceRegistry();

		MetadataSourceProcessingOrder getMetadataSourceProcessingOrder();
		NamingStrategy getNamingStrategy();
		EntityResolver getEntityResolver();
		SharedCacheMode getSharedCacheMode();
		AccessType getDefaultAccessType();
		boolean useNewIdentifierGenerators();
        boolean isGloballyQuotedIdentifiers();
		String getDefaultSchemaName();
		String getDefaultCatalogName();
		MultiTenancyStrategy getMultiTenancyStrategy();
		IndexView getJandexView();
		List<BasicType> getBasicTypeRegistrations();
	}

	/**
	 * Retrieve the options used to build this {@link Metadata} instance.
	 *
	 * @return The options.
	 */
	Options getOptions();

	/**
	 * Get the builder for {@link SessionFactory} instances based on this metamodel,
	 *
	 * @return The builder for {@link SessionFactory} instances.
	 */
	SessionFactoryBuilder getSessionFactoryBuilder();

	/**
	 * Short-hand form of building a {@link SessionFactory} through the builder without any additional
	 * option overrides.
	 *
	 * @return THe built SessionFactory.
	 */
	SessionFactory buildSessionFactory();


	EntityBinding getEntityBinding(String entityName);

	/**
	 * Get the "root" entity binding
	 * @param entityName
	 * @return the "root entity binding; simply returns entityBinding if it is the root entity binding
	 */
	EntityBinding getRootEntityBinding(String entityName);
	IdentifierGeneratorDefinition getIdGenerator(String name);
	boolean hasTypeDefinition(String name);
	TypeDefinition getTypeDefinition(String name);

	Iterable<PluralAttributeBinding> getCollectionBindings();
	Iterable<EntityBinding> getEntityBindings();
	Iterable<TypeDefinition> getTypeDefinitions();
	Iterable<NamedQueryDefinition> getNamedQueryDefinitions();
	Iterable<NamedSQLQueryDefinition> getNamedNativeQueryDefinitions();
	Iterable<FetchProfile> getFetchProfiles();

	Map<Identifier, SecondaryTable> getSecondaryTables();
	Map<String, FilterDefinition> getFilterDefinitions();
	Map<String, NamedEntityGraphDefinition> getNamedEntityGraphs();
	Map<String, ResultSetMappingDefinition> getResultSetMappingDefinitions();
	Map<String,String> getImports();
}
