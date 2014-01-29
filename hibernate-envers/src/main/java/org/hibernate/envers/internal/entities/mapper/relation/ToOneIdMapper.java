/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.envers.internal.entities.mapper.relation;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.configuration.spi.AuditConfiguration;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.entities.mapper.id.IdMapper;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.EntityTools;
import org.hibernate.envers.internal.tools.query.Parameters;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author HernпїЅn Chanfreau
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class ToOneIdMapper extends AbstractToOneMapper {
	private final IdMapper delegate;
	private final String referencedEntityName;
	private final boolean nonInsertableFake;

	public ToOneIdMapper(IdMapper delegate, PropertyData propertyData, String referencedEntityName,
						 boolean nonInsertableFake) {
		super( propertyData );
		this.delegate = delegate;
		this.referencedEntityName = referencedEntityName;
		this.nonInsertableFake = nonInsertableFake;
	}

	@Override
	public boolean mapToMapFromEntity(SessionImplementor session, Map<String, Object> data, Object newObj,
									  Object oldObj) {
		final HashMap<String, Object> newData = new HashMap<String, Object>();

		// If this property is originally non-insertable, but made insertable because it is in a many-to-one "fake"
		// bi-directional relation, we always store the "old", unchaged data, to prevent storing changes made
		// to this field. It is the responsibility of the collection to properly update it if it really changed.
		delegate.mapToMapFromEntity( newData, nonInsertableFake ? oldObj : newObj );

		for ( Map.Entry<String, Object> entry : newData.entrySet() ) {
			data.put( entry.getKey(), entry.getValue() );
		}

		return checkModified( session, newObj, oldObj );
	}

	@Override
	public void mapModifiedFlagsToMapFromEntity(SessionImplementor session, Map<String, Object> data, Object newObj,
												Object oldObj) {
		if ( getPropertyData().isUsingModifiedFlag() ) {
			data.put( getPropertyData().getModifiedFlagPropertyName(), checkModified( session, newObj, oldObj ) );
		}
	}

	@Override
	public void mapModifiedFlagsToMapForCollectionChange(String collectionPropertyName, Map<String, Object> data) {
		if ( getPropertyData().isUsingModifiedFlag() ) {
			data.put(
					getPropertyData().getModifiedFlagPropertyName(),
					collectionPropertyName.equals( getPropertyData().getName() )
			);
		}
	}

	protected boolean checkModified(SessionImplementor session, Object newObj, Object oldObj) {
		//noinspection SimplifiableConditionalExpression
		return nonInsertableFake ? false : !EntityTools.entitiesEqual( session, referencedEntityName, newObj, oldObj );
	}

	@Override
	public void nullSafeMapToEntityFromMap(AuditConfiguration verCfg, Object obj, Map data, Object primaryKey,
										   AuditReaderImplementor versionsReader, Number revision) {
		final Object entityId = delegate.mapToIdFromMap( data );
		Object value = null;
		if ( entityId != null ) {
			if ( versionsReader.getFirstLevelCache().contains( referencedEntityName, revision, entityId ) ) {
				value = versionsReader.getFirstLevelCache().get( referencedEntityName, revision, entityId );
			}
			else {
				final EntityInfo referencedEntity = getEntityInfo( verCfg, referencedEntityName );
				boolean ignoreNotFound = false;
				if ( !referencedEntity.isAudited() ) {
					final String referencingEntityName = verCfg.getEntCfg().getEntityNameForVersionsEntityName( (String) data.get( "$type$" ) );
					ignoreNotFound = verCfg.getEntCfg().getRelationDescription( referencingEntityName, getPropertyData().getName() ).isIgnoreNotFound();
				}
				if ( ignoreNotFound ) {
					// Eagerly loading referenced entity to silence potential (in case of proxy)
					// EntityNotFoundException or ObjectNotFoundException. Assigning null reference.
					value = ToOneEntityLoader.loadImmediate(
							versionsReader, referencedEntity.getEntityClass(), referencedEntityName,
							entityId, revision, RevisionType.DEL.equals( data.get( verCfg.getAuditEntCfg().getRevisionTypePropName() ) ),
							verCfg
					);
				}
				else {
					value = ToOneEntityLoader.createProxyOrLoadImmediate(
							versionsReader, referencedEntity.getEntityClass(), referencedEntityName,
							entityId, revision, RevisionType.DEL.equals( data.get( verCfg.getAuditEntCfg().getRevisionTypePropName() ) ),
							verCfg
					);
				}
			}
		}

		setPropertyValue( obj, value );
	}

	public void addMiddleEqualToQuery(Parameters parameters, String idPrefix1, String prefix1,
									  String idPrefix2, String prefix2) {
		delegate.addIdsEqualToQuery( parameters, prefix1, delegate, prefix2 );
	}
}
