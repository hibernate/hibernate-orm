/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.envers.test.integration.modifiedflags;

import java.util.List;
import java.util.Map;

import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;

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
