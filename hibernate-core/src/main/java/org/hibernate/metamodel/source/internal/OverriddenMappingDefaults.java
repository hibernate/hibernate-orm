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
package org.hibernate.metamodel.source.internal;

import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.metamodel.source.spi.MappingDefaults;

/**
 * Represents a "nested level" in the mapping defaults stack.
 *
 * @author Steve Ebersole
 */
public class OverriddenMappingDefaults implements MappingDefaults {
	private MappingDefaults overriddenValues;

	private final String packageName;
	private final String schemaName;
	private final String catalogName;
	private final String idColumnName;
	private final String tenantIdColumnName;
	private final String discriminatorColumnName;
	private final String cascade;
	private final String propertyAccess;
	private final Boolean associationLaziness;

	public OverriddenMappingDefaults(
			MappingDefaults overriddenValues,
			String packageName,
			String schemaName,
			String catalogName,
			String idColumnName,
			String tenantIdColumnName,
			String discriminatorColumnName,
			String cascade,
			String propertyAccess,
			Boolean associationLaziness) {
		if ( overriddenValues == null ) {
			throw new IllegalArgumentException( "Overridden values cannot be null" );
		}
		this.overriddenValues = overriddenValues;
		this.packageName = packageName;
		this.schemaName = schemaName;
		this.catalogName = catalogName;
		this.idColumnName = idColumnName;
		this.tenantIdColumnName = tenantIdColumnName;
		this.discriminatorColumnName = discriminatorColumnName;
		this.cascade = cascade;
		this.propertyAccess = propertyAccess;
		this.associationLaziness = associationLaziness;
	}

	@Override
	public String getPackageName() {
		return packageName == null ? overriddenValues.getPackageName() : packageName;
	}

	@Override
	public String getSchemaName() {
		return schemaName == null ? overriddenValues.getSchemaName() : schemaName;
	}

	@Override
	public String getCatalogName() {
		return catalogName == null ? overriddenValues.getCatalogName() : catalogName;
	}

	@Override
	public String getIdColumnName() {
		return idColumnName == null ? overriddenValues.getIdColumnName() : idColumnName;
	}

	@Override
	public String getTenantIdColumnName() {
		return tenantIdColumnName == null ? overriddenValues.getTenantIdColumnName() : tenantIdColumnName;
	}

	@Override
	public String getDiscriminatorColumnName() {
		return discriminatorColumnName == null ? overriddenValues.getDiscriminatorColumnName() : discriminatorColumnName;
	}

	@Override
	public String getCascadeStyle() {
		return cascade == null ? overriddenValues.getCascadeStyle() : cascade;
	}

	@Override
	public String getPropertyAccessorName() {
		return propertyAccess == null ? overriddenValues.getPropertyAccessorName() : propertyAccess;
	}

	@Override
	public boolean areAssociationsLazy() {
		return associationLaziness == null ? overriddenValues.areAssociationsLazy() : associationLaziness;
	}

	@Override
	public AccessType getCacheAccessType() {
		return overriddenValues.getCacheAccessType();
	}
}
