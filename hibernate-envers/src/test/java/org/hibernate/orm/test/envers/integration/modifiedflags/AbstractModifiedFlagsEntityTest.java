/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.modifiedflags;

import java.util.List;
import java.util.Map;

import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;

/**
 * Base test for modified flags feature
 *
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public abstract class AbstractModifiedFlagsEntityTest extends BaseEnversJPAFunctionalTestCase {
	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		if ( forceModifiedFlags() ) {
			options.put( EnversSettings.GLOBAL_WITH_MODIFIED_FLAG, "true" );
		}
	}

	public boolean forceModifiedFlags() {
		return true;
	}

	protected List queryForPropertyHasChanged(
			Class<?> clazz, Object id,
			String... propertyNames) {
		AuditQuery query = createForRevisionsQuery( clazz, id, false );
		addHasChangedProperties( query, propertyNames );
		return query.getResultList();
	}

	protected List queryForPropertyHasChangedWithDeleted(
			Class<?> clazz, Object id,
			String... propertyNames) {
		AuditQuery query = createForRevisionsQuery( clazz, id, true );
		addHasChangedProperties( query, propertyNames );
		return query.getResultList();
	}

	protected List queryForPropertyHasNotChanged(
			Class<?> clazz, Object id,
			String... propertyNames) {
		AuditQuery query = createForRevisionsQuery( clazz, id, false );
		addHasNotChangedProperties( query, propertyNames );
		return query.getResultList();
	}

	protected List queryForPropertyHasNotChangedWithDeleted(
			Class<?> clazz, Object id,
			String... propertyNames) {
		AuditQuery query = createForRevisionsQuery( clazz, id, true );
		addHasNotChangedProperties( query, propertyNames );
		return query.getResultList();
	}

	private void addHasChangedProperties(
			AuditQuery query,
			String[] propertyNames) {
		for ( String propertyName : propertyNames ) {
			query.add( AuditEntity.property( propertyName ).hasChanged() );
		}
	}

	private void addHasNotChangedProperties(
			AuditQuery query,
			String[] propertyNames) {
		for ( String propertyName : propertyNames ) {
			query.add( AuditEntity.property( propertyName ).hasNotChanged() );
		}
	}

	private AuditQuery createForRevisionsQuery(Class<?> clazz, Object id, boolean withDeleted) {
		return getAuditReader().createQuery()
				.forRevisionsOfEntity( clazz, false, withDeleted )
				.add( AuditEntity.id().eq( id ) );
	}

}
