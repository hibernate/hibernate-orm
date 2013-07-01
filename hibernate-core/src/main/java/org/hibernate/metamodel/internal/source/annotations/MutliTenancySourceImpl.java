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
package org.hibernate.metamodel.internal.source.annotations;

import org.jboss.jandex.AnnotationInstance;

import org.hibernate.metamodel.internal.source.annotations.attribute.Column;
import org.hibernate.metamodel.internal.source.annotations.attribute.FormulaValue;
import org.hibernate.metamodel.internal.source.annotations.entity.EntityClass;
import org.hibernate.metamodel.internal.source.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.source.MultiTenancySource;
import org.hibernate.metamodel.spi.source.RelationalValueSource;

/**
 * @author Steve Ebersole
 */
public class MutliTenancySourceImpl implements MultiTenancySource  {
	private final RelationalValueSource relationalValueSource;
	private final boolean shared;
	private final boolean bindAsParameter;

	public MutliTenancySourceImpl(EntityClass entityClass) {
		final AnnotationInstance columnAnnotation = JandexHelper.getSingleAnnotation(
				entityClass.getClassInfo(),
				HibernateDotNames.TENANT_COLUMN
		);
		if ( columnAnnotation != null ) {
			final Column column = new Column(  null );
			column.setName( JandexHelper.getValue( columnAnnotation, "column", String.class ) );
			column.setTable( null ); // primary table
			column.setLength( JandexHelper.getValue( columnAnnotation, "length", int.class ) );
			column.setPrecision( JandexHelper.getValue( columnAnnotation, "precision", int.class ) );
			column.setScale( JandexHelper.getValue( columnAnnotation, "scale", int.class ) );
			// todo : type
			relationalValueSource = new ColumnSourceImpl( column );
		}
		else {
			final AnnotationInstance formulaAnnotation = JandexHelper.getSingleAnnotation(
					entityClass.getClassInfo(),
					HibernateDotNames.TENANT_FORMULA
			);
			if ( formulaAnnotation != null ) {
				relationalValueSource = new DerivedValueSourceImpl(
						new FormulaValue(
								null, // primary table
								JandexHelper.getValue( formulaAnnotation, "value", String.class )
						)
				);
			}
			else {
				relationalValueSource = null;
			}
		}

		final AnnotationInstance multiTenantAnnotation = JandexHelper.getSingleAnnotation(
				entityClass.getClassInfo(),
				HibernateDotNames.MULTI_TENANT
		);
		if ( multiTenantAnnotation == null ) {
			shared = true;
			bindAsParameter = true;
		}
		else {
			shared = JandexHelper.getValue( multiTenantAnnotation, "shared", Boolean.class );
			bindAsParameter = JandexHelper.getValue( multiTenantAnnotation, "useParameterBinding", Boolean.class );
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
