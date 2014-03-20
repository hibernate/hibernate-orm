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
package org.hibernate.envers.configuration.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.envers.configuration.internal.metadata.reader.ClassAuditingData;
import org.hibernate.envers.configuration.internal.metadata.reader.PropertyAuditingData;
import org.hibernate.envers.internal.EnversMessageLogger;
import org.hibernate.envers.internal.tools.MappingTools;
import org.hibernate.metamodel.spi.binding.EntityBinding;

import org.jboss.logging.Logger;

/**
 * A helper class holding auditing meta-data for all entity bindings.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public class ClassesAuditingData {
	private static final EnversMessageLogger LOG = Logger.getMessageLogger(
			EnversMessageLogger.class,
			ClassesAuditingData.class.getName()
	);

	private final Map<String, ClassAuditingData> entityNameToAuditingData = new HashMap<String, ClassAuditingData>();
	private final Map<EntityBinding, ClassAuditingData> entityBindingToAuditingData =
			new LinkedHashMap<EntityBinding, ClassAuditingData>();

	/**
	 * Stores information about auditing meta-data for the given class.
	 *
	 * @param entityBinding The entity binding.
	 * @param cad Auditing meta-data for the given class.
	 */
	public void addClassAuditingData(EntityBinding entityBinding, ClassAuditingData cad) {
		entityNameToAuditingData.put( entityBinding.getEntityName(), cad );
		entityBindingToAuditingData.put( entityBinding, cad );
	}

	/**
	 * @return A collection of all auditing meta-data for persistent classes.
	 */
	public Collection<Map.Entry<EntityBinding, ClassAuditingData>> getAllEntityBindingAuditedData() {
		return entityBindingToAuditingData.entrySet();
	}

	/**
	 * @param entityName Name of the entity.
	 *
	 * @return Auditing meta-data for the given entity.
	 */
	public ClassAuditingData getClassAuditingData(String entityName) {
		return entityNameToAuditingData.get( entityName );
	}

	/**
	 * After all meta-data is read, updates calculated fields. This includes:
	 * <ul>
	 * <li>setting {@code forceInsertable} to {@code true} for properties specified by {@code @AuditMappedBy}</li>
	 * </ul>
	 */
	public void updateCalculatedFields() {
		for ( Map.Entry<EntityBinding, ClassAuditingData> classAuditingDataEntry : entityBindingToAuditingData.entrySet() ) {
			final EntityBinding entityBinding = classAuditingDataEntry.getKey();
			final ClassAuditingData classAuditingData = classAuditingDataEntry.getValue();
			for ( String propertyName : classAuditingData.getPropertyNames() ) {
				final PropertyAuditingData propertyAuditingData = classAuditingData.getPropertyAuditingData( propertyName );
				// If a property had the @AuditMappedBy annotation, setting the referenced fields to be always insertable.
				if ( propertyAuditingData.getAuditMappedBy() != null ) {
					final String referencedEntityName = MappingTools.getReferencedEntityName(
							entityBinding.locateAttributeBinding( propertyName )
					);

					final ClassAuditingData referencedClassAuditingData = entityNameToAuditingData.get( referencedEntityName );

					forcePropertyInsertable(
							referencedClassAuditingData, propertyAuditingData.getAuditMappedBy(),
							entityBinding.getEntityName(), referencedEntityName
					);

					forcePropertyInsertable(
							referencedClassAuditingData, propertyAuditingData.getPositionMappedBy(),
							entityBinding.getEntityName(), referencedEntityName
					);
				}
			}
		}
	}

	private void forcePropertyInsertable(
			ClassAuditingData classAuditingData, String propertyName,
			String entityName, String referencedEntityName) {
		if ( propertyName != null ) {
			if ( classAuditingData.getPropertyAuditingData( propertyName ) == null ) {
				throw new MappingException(
						"@AuditMappedBy points to a property that doesn't exist: " +
								referencedEntityName + "." + propertyName
				);
			}

			LOG.debugf(
					"Non-insertable property %s.%s will be made insertable because a matching @AuditMappedBy was found in the %s entity",
					referencedEntityName,
					propertyName,
					entityName
			);

			classAuditingData
					.getPropertyAuditingData( propertyName )
					.setForceInsertable( true );
		}
	}
}
