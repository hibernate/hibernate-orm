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

import java.util.Map;

import org.hibernate.cfg.NamingStrategy;
import org.hibernate.metamodel.domain.MetaAttribute;
import org.hibernate.metamodel.source.spi.MappingDefaults;
import org.hibernate.service.ServiceRegistry;

/**
 * @author Steve Ebersole
 */
public class OverriddenMappingDefaults implements MappingDefaults {
	private MappingDefaults overriddenValues;

	private final String packageName;
	private final String schemaName;
	private final String catalogName;
	private final String idColumnName;
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
	public String getDefaultSchemaName() {
		return schemaName == null ? overriddenValues.getDefaultSchemaName() : schemaName;
	}

	@Override
	public String getDefaultCatalogName() {
		return catalogName == null ? overriddenValues.getDefaultCatalogName() : catalogName;
	}

	@Override
	public String getDefaultIdColumnName() {
		return idColumnName == null ? overriddenValues.getDefaultIdColumnName() : idColumnName;
	}

	@Override
	public String getDefaultDiscriminatorColumnName() {
		return discriminatorColumnName == null ? overriddenValues.getDefaultDiscriminatorColumnName() : discriminatorColumnName;
	}

	@Override
	public String getDefaultCascade() {
		return cascade == null ? overriddenValues.getDefaultCascade() : cascade;
	}

	@Override
	public String getDefaultAccess() {
		return propertyAccess == null ? overriddenValues.getDefaultAccess() : propertyAccess;
	}

	@Override
	public boolean isDefaultLazy() {
		return associationLaziness == null ? overriddenValues.isDefaultLazy() : associationLaziness;
	}

	@Override
	public Map<String, MetaAttribute> getMappingMetas() {
		return null; // todo : implement method body
	}
}
