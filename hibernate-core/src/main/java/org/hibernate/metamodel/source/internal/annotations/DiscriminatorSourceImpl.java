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
package org.hibernate.metamodel.source.internal.annotations;

import javax.persistence.DiscriminatorType;

import org.hibernate.AnnotationException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.metamodel.source.internal.annotations.attribute.Column;
import org.hibernate.metamodel.source.internal.annotations.entity.EntityTypeMetadata;
import org.hibernate.metamodel.source.internal.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JandexHelper;
import org.hibernate.metamodel.source.spi.DiscriminatorSource;
import org.hibernate.metamodel.source.spi.RelationalValueSource;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

/**
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 */
public class DiscriminatorSourceImpl
		extends DiscriminatorSourceSupport
		implements DiscriminatorSource {
	private final String entityName;
	private final Class javaType;
	private final RelationalValueSource relationalValueSource;

	private final boolean isFormula;

	public DiscriminatorSourceImpl(EntityTypeMetadata entityTypeMetadata) {
		super( entityTypeMetadata );
		this.entityName = entityTypeMetadata.getJavaTypeDescriptor().getName().toString();

		final ClassLoaderService cls = entityTypeMetadata.getLocalBindingContext()
				.getServiceRegistry()
				.getService( ClassLoaderService.class );

		final AnnotationInstance discriminatorColumn = entityTypeMetadata.getJavaTypeDescriptor()
				.findTypeAnnotation( JPADotNames.DISCRIMINATOR_COLUMN );
		final AnnotationInstance discriminatorFormula = entityTypeMetadata.getJavaTypeDescriptor()
				.findTypeAnnotation( HibernateDotNames.DISCRIMINATOR_FORMULA );

		DiscriminatorType discriminatorType = DiscriminatorType.STRING;
		if ( discriminatorColumn != null && discriminatorFormula != null ) {
			// while it is obviously illegal to request both a column and a formula
			// as the discriminator, there is a feature here where we allow the
			// @DiscriminatorColumn to be used to specify the DiscriminatorType
			// for a @DiscriminatorFormula.  So the combo is valid as long as
			// the @DiscriminatorColumn does not name a column
			final AnnotationValue columnNameValue = discriminatorColumn.value( "name" );
			// NOTE : Jandex presents values that have not been specified as null, rather
			// than returning the default value.  That's actually great for cases like this :)
			if ( columnNameValue != null ) {
				throw entityTypeMetadata.getLocalBindingContext().makeMappingException(
						"Entity hierarchy [" + entityTypeMetadata.getJavaTypeDescriptor().getName().toString() +
								"] declared both a @DiscriminatorColumn and a @DiscriminatorFormula " +
								"which are mutually exclusive"
				);
			}
		}

		this.isFormula = discriminatorFormula != null;
		if ( isFormula ) {
			final String expression = entityTypeMetadata.getLocalBindingContext()
					.getJandexAccess()
					.getTypedValueExtractor( String.class )
					.extract( discriminatorFormula, "value" );
			this.relationalValueSource = new DerivedValueSourceImpl( expression, null );
		}
		else {
			discriminatorType = entityTypeMetadata.getLocalBindingContext()
					.getJandexAccess()
					.getTypedValueExtractor( DiscriminatorType.class )
					.extract( discriminatorColumn, "discriminatorType" );

			final Column column = new Column( null );
			column.setNullable( false );
			column.setName(
					entityTypeMetadata.getLocalBindingContext()
							.getJandexAccess()
							.getTypedValueExtractor( String.class )
							.extract( discriminatorColumn, "name" )
			);
			column.setLength(
					entityTypeMetadata.getLocalBindingContext()
							.getJandexAccess()
							.getTypedValueExtractor( Integer.class )
							.extract( discriminatorColumn, "length" )
			);
			column.setColumnDefinition(
					entityTypeMetadata.getLocalBindingContext()
							.getJandexAccess()
							.getTypedValueExtractor( String.class )
							.extract( discriminatorColumn, "columnDefinition" )
			);

			this.relationalValueSource = new ColumnSourceImpl( column );
		}

		switch ( discriminatorType ) {
			case STRING: {
				javaType = String.class;
				break;
			}
			case CHAR: {
				javaType = Character.class;
				break;
			}
			case INTEGER: {
				javaType = Integer.class;
				break;
			}
			default: {
				throw new AnnotationException( "Unsupported discriminator type: " + discriminatorType );
			}
		}
	}

	@Override
	public RelationalValueSource getDiscriminatorRelationalValueSource() {
		return relationalValueSource;
	}

	@Override
	public String getExplicitHibernateTypeName() {
		return javaType.getName();
	}

	@Override
	public boolean isInserted() {
		return !isFormula && super.isInserted();
	}

	@Override
	public String toString() {
		return "DiscriminatorSourceImpl{entity=" + entityName + '}';
	}
}
