/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.modifiedflags;

import java.util.List;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.testing.envers.junit.EnversTest;

/**
 * Base utility for modified flags feature tests
 *
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
@EnversTest
public abstract class AbstractModifiedFlagsEntityTest {

	protected static List queryForPropertyHasChanged(
			AuditReader auditReader, Class<?> clazz, Object id,
			String... propertyNames) {
		AuditQuery query = createForRevisionsQuery( auditReader, clazz, id, false );
		addHasChangedProperties( query, propertyNames );
		return query.getResultList();
	}

	protected static List queryForPropertyHasChangedWithDeleted(
			AuditReader auditReader, Class<?> clazz, Object id,
			String... propertyNames) {
		AuditQuery query = createForRevisionsQuery( auditReader, clazz, id, true );
		addHasChangedProperties( query, propertyNames );
		return query.getResultList();
	}

	protected static List queryForPropertyHasNotChanged(
			AuditReader auditReader, Class<?> clazz, Object id,
			String... propertyNames) {
		AuditQuery query = createForRevisionsQuery( auditReader, clazz, id, false );
		addHasNotChangedProperties( query, propertyNames );
		return query.getResultList();
	}

	protected static List queryForPropertyHasNotChangedWithDeleted(
			AuditReader auditReader, Class<?> clazz, Object id,
			String... propertyNames) {
		AuditQuery query = createForRevisionsQuery( auditReader, clazz, id, true );
		addHasNotChangedProperties( query, propertyNames );
		return query.getResultList();
	}

	private static void addHasChangedProperties(
			AuditQuery query,
			String[] propertyNames) {
		for ( String propertyName : propertyNames ) {
			query.add( AuditEntity.property( propertyName ).hasChanged() );
		}
	}

	private static void addHasNotChangedProperties(
			AuditQuery query,
			String[] propertyNames) {
		for ( String propertyName : propertyNames ) {
			query.add( AuditEntity.property( propertyName ).hasNotChanged() );
		}
	}

	private static AuditQuery createForRevisionsQuery(AuditReader auditReader, Class<?> clazz, Object id, boolean withDeleted) {
		return auditReader.createQuery()
				.forRevisionsOfEntity( clazz, false, withDeleted )
				.add( AuditEntity.id().eq( id ) );
	}

}
