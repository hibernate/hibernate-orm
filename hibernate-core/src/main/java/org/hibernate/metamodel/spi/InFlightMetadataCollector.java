/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.spi;

import org.hibernate.cfg.ObjectNameNormalizer;
import org.hibernate.cfg.annotations.NamedEntityGraphDefinition;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.metamodel.Metadata;
import org.hibernate.metamodel.NamedStoredProcedureQueryDefinition;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.FetchProfile;
import org.hibernate.metamodel.spi.binding.IdentifierGeneratorDefinition;
import org.hibernate.metamodel.spi.binding.PluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.SecondaryTable;
import org.hibernate.metamodel.spi.binding.TypeDefinition;
import org.hibernate.metamodel.spi.relational.Database;
import org.hibernate.procedure.ProcedureCallMemento;
import org.hibernate.type.TypeResolver;

/**
 * @author Steve Ebersole
 */
public interface InFlightMetadataCollector extends Mapping, Metadata {

	Database getDatabase();

	TypeResolver getTypeResolver();

	ObjectNameNormalizer getObjectNameNormalizer();

	void addImport(String entityName, String entityName1);

	void addEntity(EntityBinding entityBinding);

	void addSecondaryTable(SecondaryTable secondaryTable);

	PluralAttributeBinding getCollection(String role);

	void addCollection(PluralAttributeBinding collectionBinding);

	void addFetchProfile(FetchProfile profile);

	void addTypeDefinition(TypeDefinition typeDefinition);

	void addFilterDefinition(FilterDefinition filterDefinition);

	void addIdGenerator(IdentifierGeneratorDefinition generator);

	void registerIdentifierGenerator(String name, String clazz);

	void addNamedNativeQuery(NamedSQLQueryDefinition def);

	void addNamedEntityGraph(NamedEntityGraphDefinition def);

	void addNamedQuery(NamedQueryDefinition def);

	void addNamedStoredProcedureQueryDefinition(NamedStoredProcedureQueryDefinition definition);

	void addResultSetMapping(ResultSetMappingDefinition resultSetMappingDefinition);

	@Deprecated
	void setGloballyQuotedIdentifiers(boolean b);
}
