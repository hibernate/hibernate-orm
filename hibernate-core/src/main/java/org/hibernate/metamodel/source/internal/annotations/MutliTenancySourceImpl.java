/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.internal.annotations;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.metamodel.source.internal.annotations.attribute.Column;
import org.hibernate.metamodel.source.internal.annotations.attribute.FormulaValue;
import org.hibernate.metamodel.source.internal.annotations.entity.EntityTypeMetadata;
import org.hibernate.metamodel.source.internal.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JandexHelper;
import org.hibernate.metamodel.source.spi.MultiTenancySource;
import org.hibernate.metamodel.source.spi.RelationalValueSource;

import org.jboss.jandex.AnnotationInstance;

/**
 * @author Steve Ebersole
 */
public class MutliTenancySourceImpl implements MultiTenancySource  {
	private final RelationalValueSource relationalValueSource;
	private final boolean shared;
	private final boolean bindAsParameter;

	public MutliTenancySourceImpl(EntityTypeMetadata entityTypeMetadata) {
		final ClassLoaderService classLoaderService = entityTypeMetadata.getLocalBindingContext()
				.getBuildingOptions()
				.getServiceRegistry()
				.getService( ClassLoaderService.class );

		final AnnotationInstance columnAnnotation = entityTypeMetadata.getJavaTypeDescriptor().findTypeAnnotation(				HibernateDotNames.TENANT_COLUMN
		);
		if ( columnAnnotation != null ) {
			final Column column = new Column(  null );
			column.setName( JandexHelper.getValue( columnAnnotation, "column", String.class, classLoaderService ) );
			column.setTable( null ); // primary table
			column.setLength( JandexHelper.getValue( columnAnnotation, "length", int.class, classLoaderService ) );
			column.setPrecision( JandexHelper.getValue( columnAnnotation, "precision", int.class, classLoaderService ) );
			column.setScale( JandexHelper.getValue( columnAnnotation, "scale", int.class, classLoaderService ) );
			// todo : type
			relationalValueSource = new ColumnSourceImpl( column );
		}
		else {
			final AnnotationInstance formulaAnnotation = entityTypeMetadata.getJavaTypeDescriptor().findTypeAnnotation(
					HibernateDotNames.TENANT_FORMULA
			);
			if ( formulaAnnotation != null ) {
				relationalValueSource = new DerivedValueSourceImpl(
						new FormulaValue(
								null, // primary table
								JandexHelper.getValue( formulaAnnotation, "value", String.class, classLoaderService )
						)
				);
			}
			else {
				relationalValueSource = null;
			}
		}

		final AnnotationInstance multiTenantAnnotation = entityTypeMetadata.getJavaTypeDescriptor().findTypeAnnotation(
				HibernateDotNames.MULTI_TENANT
		);
		if ( multiTenantAnnotation == null ) {
			shared = true;
			bindAsParameter = true;
		}
		else {
			shared = JandexHelper.getValue( multiTenantAnnotation, "shared", Boolean.class, classLoaderService );
			bindAsParameter = JandexHelper.getValue( multiTenantAnnotation, "useParameterBinding", Boolean.class, classLoaderService );
		}
	}

	@Override
	public RelationalValueSource getRelationalValueSource() {
		return relationalValueSource;
	}

	@Override
	public boolean isShared() {
		return shared;
	}

	@Override
	public boolean bindAsParameter() {
		return bindAsParameter;
	}
}
