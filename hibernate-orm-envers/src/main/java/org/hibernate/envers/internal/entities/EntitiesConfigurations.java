/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Configuration of the user entities: property mapping of the entities, relations, inheritance.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Hern&aacute;n Chanfreau
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class EntitiesConfigurations {
	private Map<String, EntityConfiguration> entitiesConfigurations;
	private Map<String, EntityConfiguration> notAuditedEntitiesConfigurations;

	// Map versions entity name -> entity name
	private Map<String, String> entityNamesForVersionsEntityNames = new HashMap<>();

	public EntitiesConfigurations(
			Map<String, EntityConfiguration> entitiesConfigurations,
			Map<String, EntityConfiguration> notAuditedEntitiesConfigurations) {
		this.entitiesConfigurations = entitiesConfigurations;
		this.notAuditedEntitiesConfigurations = notAuditedEntitiesConfigurations;

		generateBidirectionRelationInfo();
		generateVersionsEntityToEntityNames();
	}

	private void generateVersionsEntityToEntityNames() {
		entityNamesForVersionsEntityNames = new HashMap<>();
		for ( Map.Entry<String, EntityConfiguration> entry : entitiesConfigurations.entrySet() ) {
			entityNamesForVersionsEntityNames.put( entry.getValue().getVersionsEntityName(), entry.getKey() );
		}
	}

	private void generateBidirectionRelationInfo() {
		// Checking each relation if it is bidirectional. If so, storing that information.
		for ( Map.Entry<String, EntityConfiguration> entry : entitiesConfigurations.entrySet() ) {
			final String entityName = entry.getKey();
			final EntityConfiguration entCfg = entry.getValue();
			// Iterating over all relations from that entity
			for ( RelationDescription relDesc : entCfg.getRelationsIterator() ) {
				// If this is an "owned" relation, checking the related entity, if it has a relation that has
				// a mapped-by attribute to the currently checked. If so, this is a bidirectional relation.
				if ( relDesc.getRelationType() == RelationType.TO_ONE ||
						relDesc.getRelationType() == RelationType.TO_MANY_MIDDLE ) {
					final EntityConfiguration entityConfiguration = entitiesConfigurations.get( relDesc.getToEntityName() );
					if ( entityConfiguration != null ) {
						for ( RelationDescription other : entityConfiguration.getRelationsIterator() ) {
							if ( relDesc.getFromPropertyName().equals( other.getMappedByPropertyName() ) &&
									(entityName.equals( other.getToEntityName() )) ) {
								relDesc.setBidirectional( true );
								other.setBidirectional( true );
							}
						}
					}
				}
			}
		}
	}

	public EntityConfiguration get(String entityName) {
		return entitiesConfigurations.get( entityName );
	}

	public EntityConfiguration getNotVersionEntityConfiguration(String entityName) {
		return notAuditedEntitiesConfigurations.get( entityName );
	}

	public String getEntityNameForVersionsEntityName(String versionsEntityName) {
		return entityNamesForVersionsEntityNames.get( versionsEntityName );
	}

	public boolean isVersioned(String entityName) {
		return get( entityName ) != null;
	}

	public boolean hasAuditedEntities() {
		return entitiesConfigurations.size() != 0;
	}

	public RelationDescription getRelationDescription(String entityName, String propertyName) {
		final EntityConfiguration entCfg;
		if ( isVersioned( entityName ) ) {
			entCfg = get( entityName );
		}
		else {
			entCfg = getNotVersionEntityConfiguration( entityName );
		}
		final RelationDescription relDesc = entCfg.getRelationDescription( propertyName );
		if ( relDesc != null ) {
			return relDesc;
		}
		else if ( entCfg.getParentEntityName() != null ) {
			// The field may be declared in a superclass ...
			return getRelationDescription( entCfg.getParentEntityName(), propertyName );
		}
		else {
			return null;
		}
	}

	private Collection<RelationDescription> getRelationDescriptions(String entityName) {
		final EntityConfiguration entCfg = entitiesConfigurations.get( entityName );
		Collection<RelationDescription> descriptions = new ArrayList<>();
		if ( entCfg.getParentEntityName() != null ) {
			// collect descriptions from super classes
			descriptions.addAll( getRelationDescriptions( entCfg.getParentEntityName() ) );
		}
		for ( RelationDescription relationDescription : entCfg.getRelationsIterator() ) {
			descriptions.add( relationDescription );
		}
		return descriptions;
	}

	private void addWithParentEntityNames(String entityName, Set<String> entityNames) {
		entityNames.add( entityName );
		final EntityConfiguration entCfg = entitiesConfigurations.get( entityName );
		if ( entCfg.getParentEntityName() != null ) {
			// collect descriptions from super classes
			addWithParentEntityNames( entCfg.getParentEntityName(), entityNames );
		}
	}

	private Set<String> getEntityAndParentsNames(String entityName) {
		final Set<String> names = new HashSet<>();
		addWithParentEntityNames( entityName, names );
		return names;
	}

	public Set<String> getToPropertyNames(String fromEntityName, String fromPropertyName, String toEntityName) {
		final Set<String> entityAndParentsNames = getEntityAndParentsNames( fromEntityName );
		final Set<String> toPropertyNames = new HashSet<>();
		for ( RelationDescription relationDescription : getRelationDescriptions( toEntityName ) ) {
			final String relToEntityName = relationDescription.getToEntityName();
			final String mappedByPropertyName = relationDescription.getMappedByPropertyName();
			if ( entityAndParentsNames.contains( relToEntityName ) && mappedByPropertyName != null && mappedByPropertyName
					.equals( fromPropertyName ) ) {
				toPropertyNames.add( relationDescription.getFromPropertyName() );
			}
		}
		return toPropertyNames;
	}
}
