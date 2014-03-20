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

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import javax.persistence.NamedStoredProcedureQuery;

import org.hibernate.SessionFactory;
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

/**
 * @author Steve Ebersole
 */
public interface Metadata {
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

	/**
	 * Gets the {@link UUID} for this metamodel.
	 *
	 * @return the UUID.
	 */
	UUID getUUID();

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
	Iterable<FetchProfile> getFetchProfiles();

	Map<Identifier, SecondaryTable> getSecondaryTables();
	Map<String, FilterDefinition> getFilterDefinitions();
	Map<String, NamedEntityGraphDefinition> getNamedEntityGraphs();

	Map<String,String> getImports();

	NamedSQLQueryDefinition getNamedNativeQuery(String name);
	Iterable<NamedQueryDefinition> getNamedQueryDefinitions();
	Iterable<NamedSQLQueryDefinition> getNamedNativeQueryDefinitions();
	Collection<NamedStoredProcedureQueryDefinition> getNamedStoredProcedureQueryDefinitions();
	Map<String, ResultSetMappingDefinition> getResultSetMappingDefinitions();
}
