/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.EntityNameResolver;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.MappedSuperclassDomainType;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class InflightRuntimeMetamodel {

	private final TypeConfiguration typeConfiguration;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Hibernate metamodel

	private final Map<String, EntityPersister> entityPersisterMap = new HashMap<>();
	private Map<Class, String> entityProxyInterfaceMap;
	private Map<String, CollectionPersister> collectionPersisterMap;
	private Map<String, Set<String>> collectionRolesByEntityParticipant;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA metamodel

	private final Map<String, EntityDomainType<?>> jpaEntityTypeMap = new HashMap<>();
	private Map<Class, MappedSuperclassDomainType<?>> jpaMappedSuperclassTypeMap;
	private Map<Class, EmbeddableDomainType<?>> embeddableDescriptorMap;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Misc

	private final Map<String, String> nameToImportNameMap = new HashMap<>();
	private Set<EntityNameResolver> entityNameResolvers;


	public InflightRuntimeMetamodel(TypeConfiguration typeConfiguration) {
		this.typeConfiguration = typeConfiguration;
	}
}
