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

package org.hibernate.metamodel.source.annotations.attribute.type;

import java.util.Calendar;
import java.util.Date;
import javax.persistence.TemporalType;

import org.jboss.jandex.AnnotationInstance;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.metamodel.source.annotations.JPADotNames;
import org.hibernate.metamodel.source.annotations.JandexHelper;
import org.hibernate.metamodel.source.annotations.attribute.MappedAttribute;
import org.hibernate.type.StandardBasicTypes;

/**
 * @author Strong Liu
 */
public class TemporalTypeResolver extends AbstractAttributeTypeResolver {
	private final MappedAttribute mappedAttribute;
	private final boolean isMapKey;

	public TemporalTypeResolver(MappedAttribute mappedAttribute) {
		if ( mappedAttribute == null ) {
			throw new AssertionFailure( "MappedAttribute is null" );
		}
		this.mappedAttribute = mappedAttribute;
		this.isMapKey = false;//todo
	}

	@Override
	public String resolveHibernateTypeName(AnnotationInstance temporalAnnotation) {

		if ( isTemporalType( mappedAttribute.getAttributeType() ) ) {
			if ( temporalAnnotation == null ) {
				//SPEC 11.1.47 The Temporal annotation must be specified for persistent fields or properties of type java.util.Date and java.util.Calendar.
				throw new AnnotationException( "Attribute " + mappedAttribute.getName() + " is a Temporal type, but no @Temporal annotation found." );
			}
			TemporalType temporalType = JandexHelper.getEnumValue( temporalAnnotation, "value", TemporalType.class );
			boolean isDate = Date.class.isAssignableFrom( mappedAttribute.getAttributeType() );
			String type;
			switch ( temporalType ) {
				case DATE:
					type = isDate ? StandardBasicTypes.DATE.getName() : StandardBasicTypes.CALENDAR_DATE.getName();
					break;
				case TIME:
					type = StandardBasicTypes.TIME.getName();
					if ( !isDate ) {
						throw new NotYetImplementedException( "Calendar cannot persist TIME only" );
					}
					break;
				case TIMESTAMP:
					type = isDate ? StandardBasicTypes.TIMESTAMP.getName() : StandardBasicTypes.CALENDAR.getName();
					break;
				default:
					throw new AssertionFailure( "Unknown temporal type: " + temporalType );
			}
			return type;
		}
		else {
			if ( temporalAnnotation != null ) {
				throw new AnnotationException(
						"@Temporal should only be set on a java.util.Date or java.util.Calendar property: " + mappedAttribute
								.getName()
				);
			}
		}
		return null;
	}

	@Override
	protected AnnotationInstance getTypeDeterminingAnnotationInstance() {
		return JandexHelper.getSingleAnnotation(
				mappedAttribute.annotations(),
				JPADotNames.TEMPORAL
		);
	}

	private static boolean isTemporalType(Class type) {
		return Date.class.isAssignableFrom( type ) || Calendar.class.isAssignableFrom( type );
	}
}
