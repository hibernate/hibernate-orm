/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.modifiedflags;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.NonIdPersistentAttribute;

/**
 * Base test for modified flags feature
 *
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public abstract class AbstractModifiedFlagsEntityTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	@Override
	protected void addSettings(Map<String, Object> settings) {
		super.addSettings( settings );

		if ( forceModifiedFlags() ) {
			settings.put( EnversSettings.GLOBAL_WITH_MODIFIED_FLAG, Boolean.TRUE.toString() );
		}
	}

	public boolean forceModifiedFlags() {
		return true;
	}

	protected List queryForPropertyHasChanged(Class<?> clazz, Object id, String... propertyNames) {
		AuditQuery query = createForRevisionsQuery( clazz, id, false );
		addHasChangedProperties( query, propertyNames );
		return query.getResultList();
	}

	protected List queryForPropertyHasChangedWithDeleted(Class<?> clazz, Object id, String... propertyNames) {
		AuditQuery query = createForRevisionsQuery( clazz, id, true );
		addHasChangedProperties( query, propertyNames );
		return query.getResultList();
	}

	protected List queryForPropertyHasNotChanged(Class<?> clazz, Object id, String... propertyNames) {
		AuditQuery query = createForRevisionsQuery( clazz, id, false );
		addHasNotChangedProperties( query, propertyNames );
		return query.getResultList();
	}

	protected List queryForPropertyHasNotChangedWithDeleted(Class<?> clazz, Object id, String... propertyNames) {
		AuditQuery query = createForRevisionsQuery( clazz, id, true );
		addHasNotChangedProperties( query, propertyNames );
		return query.getResultList();
	}

	private void addHasChangedProperties(AuditQuery query, String[] propertyNames) {
		for ( String propertyName : propertyNames ) {
			query.add( AuditEntity.property( propertyName ).hasChanged() );
		}
	}

	private void addHasNotChangedProperties(AuditQuery query, String[] propertyNames) {
		for ( String propertyName : propertyNames ) {
			query.add( AuditEntity.property( propertyName ).hasNotChanged() );
		}
	}

	private AuditQuery createForRevisionsQuery(Class<?> clazz, Object id, boolean withDeleted) {
		return getAuditReader().createQuery()
				.forRevisionsOfEntity( clazz, false, withDeleted )
				.add( AuditEntity.id().eq( id ) );
	}

	/**
	 * Returns a list of revision numbers where the specified properties were modified for an entity.
	 *
	 * @param clazz The entity class.
	 * @param id The entity primary key.
	 * @param propertyNames The property names to check.
	 *
	 * @return List of revision numbers.
	 */
	@SuppressWarnings("WeakerAccess")
	protected List<Number> getPropertyChangeRevisions(Class<?> clazz, Object id, String... propertyNames) {
		final List<Number> results = new ArrayList<>();

		AuditQuery auditQuery = createForRevisionsQuery( clazz, id, false );
		addHasChangedProperties( auditQuery, propertyNames );

		final List queryResults = auditQuery.getResultList();
		for ( Object queryResult : queryResults ) {
			results.add( ( (SequenceIdRevisionEntity) ( (Object[]) queryResult )[1] ).getId() );
		}

		return results;
	}

	/**
	 * Extract all persistent attributes of the specified entity that end with the specified suffix.
	 *
	 * @param entityName The entity name.
	 * @param suffix The suffix to filter persistent attributes by.
	 *
	 * @return List of persistent attributes that match the specified suffix.
	 */
	protected List<String> extractModifiedPropertyNames(String entityName, String suffix) {
		return getMetamodel().getEntityDescriptor( entityName )
				.getPersistentAttributes()
				.stream()
				.map( NonIdPersistentAttribute::getName )
				.filter( name -> name.endsWith( suffix ) )
				.collect( Collectors.toList() );
	}

	@SuppressWarnings("WeakerAccess")
	protected static List<Integer> extractRevisions(List queryResults) {
		final List<Integer> results = new ArrayList<>();
		for ( Object queryResult : queryResults ) {
			results.add( ( (SequenceIdRevisionEntity) ( (Object[]) queryResult )[ 1 ] ).getId() );
		}
		return results;
	}
}
