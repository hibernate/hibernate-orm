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
package org.hibernate.metamodel.source;

import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.metamodel.Metadata;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.FetchProfile;
import org.hibernate.metamodel.binding.IdGenerator;
import org.hibernate.metamodel.binding.PluralAttributeBinding;
import org.hibernate.metamodel.binding.TypeDef;
import org.hibernate.metamodel.relational.Database;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.TypeResolver;

/**
 * @author Steve Ebersole
 */
public interface MetadataImplementor extends Metadata, BindingContext, Mapping {
	public ServiceRegistry getServiceRegistry();

	public Database getDatabase();

	public TypeResolver getTypeResolver();

	public void addImport(String entityName, String entityName1);

	public void addEntity(EntityBinding entityBinding);

	public void addCollection(PluralAttributeBinding collectionBinding);

	public void addFetchProfile(FetchProfile profile);

	public void addTypeDefinition(TypeDef typeDef);

	public void addFilterDefinition(FilterDefinition filterDefinition);

	public void addIdGenerator(IdGenerator generator);

	public void registerIdentifierGenerator(String name, String clazz);

	public void addNamedNativeQuery(NamedSQLQueryDefinition def);

	public void addNamedQuery(NamedQueryDefinition def);

	public void addResultSetMapping(ResultSetMappingDefinition resultSetMappingDefinition);

	// todo : this needs to move to AnnotationBindingContext
	public void setGloballyQuotedIdentifiers(boolean b);

	public MetaAttributeContext getGlobalMetaAttributeContext();
}
