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

package org.hibernate.metamodel.source.internal.annotations.attribute.type;

import javax.persistence.TemporalType;

import org.hibernate.annotations.SourceType;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.source.internal.annotations.attribute.BasicAttribute;
import org.hibernate.metamodel.source.internal.annotations.attribute.PluralAttribute;
import org.hibernate.metamodel.source.internal.annotations.entity.EntityBindingContext;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.type.StandardBasicTypes;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

/**
 * @author Strong Liu
 * @author Brett Meyer
 * @author Gail Badner
 */
public class TemporalTypeResolver extends AbstractAttributeTypeResolver {
	private final SourceType versionSourceType;

	public static TemporalTypeResolver createAttributeTypeResolver(BasicAttribute attribute) {
		return new TemporalTypeResolver(
				attribute.getName(),
				attribute.getBackingMember().getType().getErasedType(),
				attribute.getBackingMember().getAnnotations().get( JPADotNames.TEMPORAL ),
				attribute.getContext(),
				attribute.getVersionSourceType()
		);
	}

	public static TemporalTypeResolver createCollectionElementTypeResolver(PluralAttribute attribute, JavaTypeDescriptor elementType) {
		return new TemporalTypeResolver(
				attribute.getName(),
				elementType,
				attribute.getBackingMember().getAnnotations().get( JPADotNames.TEMPORAL ),
				attribute.getContext(),
				null
		);
	}

	public static TemporalTypeResolver createCollectionIndexTypeResolver(PluralAttribute attribute, JavaTypeDescriptor mapKeyType) {
		return new TemporalTypeResolver(
				attribute.getName(),
				mapKeyType,
				attribute.getBackingMember().getAnnotations().get( JPADotNames.MAP_KEY_TEMPORAL ),
				attribute.getContext(),
				null
		);
	}

	private final TemporalNature nature;

	private TemporalTypeResolver(
			String name,
			JavaTypeDescriptor javaType,
			AnnotationInstance annotation,
			EntityBindingContext context,
			SourceType versionSourceType) {
		super( name, javaType, annotation, context );

		this.versionSourceType = versionSourceType;

		this.nature = interpretNature( javaType );
		if ( nature == TemporalNature.NONE ) {
			if ( annotation != null ) {
				throw context.makeMappingException(
						'@' + annotation.name().toString() + " should only be used on temporal values : " + name()
				);
			}
		}
	}

	private TemporalNature interpretNature(JavaTypeDescriptor javaType) {
		if ( calendarType().isAssignableFrom( javaType ) ) {
			return TemporalNature.CALENDAR;
		}
		else if ( jdbcTimestampType().isAssignableFrom( javaType ) ) {
			return TemporalNature.JDBC_TIMESTAMP;
		}
		else if ( jdbcTimeType().isAssignableFrom( javaType ) ) {
			return TemporalNature.JDBC_TIME;
		}
		else if ( jdbcDateType().isAssignableFrom( javaType ) ) {
			return TemporalNature.JDBC_DATE;
		}
		else if ( jdkDateType().isAssignableFrom( javaType ) ) {
			return TemporalNature.JDK_DATE;
		}
		else {
			return TemporalNature.NONE;
		}
	}

	private JavaTypeDescriptor calendarDescriptor;

	private JavaTypeDescriptor calendarType() {
		if ( calendarDescriptor == null ) {
			calendarDescriptor = getContext().getJavaTypeDescriptorRepository().getType(
					DotName.createSimple( java.util.Calendar.class.getName() )
			);
		}
		return calendarDescriptor;
	}

	private JavaTypeDescriptor jdbcTimestampDescriptor;

	private JavaTypeDescriptor jdbcTimestampType() {
		if ( jdbcTimestampDescriptor == null ) {
			jdbcTimestampDescriptor = getContext().getJavaTypeDescriptorRepository().getType(
					DotName.createSimple( java.sql.Timestamp.class.getName() )
			);
		}
		return jdbcTimestampDescriptor;
	}

	private JavaTypeDescriptor jdbcTimeDescriptor;

	private JavaTypeDescriptor jdbcTimeType() {
		if ( jdbcTimeDescriptor == null ) {
			jdbcTimeDescriptor = getContext().getJavaTypeDescriptorRepository().getType(
					DotName.createSimple( java.sql.Time.class.getName() )
			);
		}
		return jdbcTimeDescriptor;
	}

	private JavaTypeDescriptor jdbcDateDescriptor;

	private JavaTypeDescriptor jdbcDateType() {
		if ( jdbcDateDescriptor == null ) {
			jdbcDateDescriptor = getContext().getJavaTypeDescriptorRepository().getType(
					DotName.createSimple( java.sql.Date.class.getName() )
			);
		}
		return jdbcDateDescriptor;
	}

	private JavaTypeDescriptor jdkDateDescriptor;

	private JavaTypeDescriptor jdkDateType() {
		if ( jdkDateDescriptor == null ) {
			jdkDateDescriptor = getContext().getJavaTypeDescriptorRepository().getType(
					DotName.createSimple( java.util.Date.class.getName() )
			);
		}
		return jdkDateDescriptor;
	}

	@Override
	public String resolveHibernateTypeName() {
		if ( nature == TemporalNature.NONE ) {
			return null;
		}

		if ( versionSourceType != null ) {
			return versionSourceType.typeName();
		}

		if ( annotation() == null ) {
			// Although JPA 2.1 states that @Temporal is required on
			// Date/Calendar attributes, we allow it to be left off in order
			// to support legacy mappings using the following mappings:
			// 		java.util.Calendar -> CalendarType
			// 		java.sql.Date -> DateType
			// 		java.sql.Time -> TimeType
			// 		java.util.Date -> TimestampType
			// 		java.sql.Timestamp -> TimestampType
			switch ( nature ) {
				case CALENDAR: {
					return StandardBasicTypes.CALENDAR.getName();
				}
				case JDBC_DATE: {
					return StandardBasicTypes.DATE.getName();
				}
				case JDBC_TIME: {
					return StandardBasicTypes.TIME.getName();
				}
				case JDK_DATE:
				case JDBC_TIMESTAMP: {
					return StandardBasicTypes.TIMESTAMP.getName();
				}
				default: {
					// java is funny sometimes ;)
					return null;
				}
			}
		}
		else {
			final TemporalType temporalType = TemporalType.valueOf( annotation().value().asEnum() );
			final boolean isCalendar = nature == TemporalNature.CALENDAR;
			switch ( temporalType ) {
				case TIMESTAMP: {
					return isCalendar
							? StandardBasicTypes.CALENDAR.getName()
							: StandardBasicTypes.TIMESTAMP.getName();
				}
				case TIME: {
					return isCalendar
							? StandardBasicTypes.CALENDAR_TIME.getName()
							: StandardBasicTypes.TIME.getName();
				}
				case DATE: {
					return isCalendar
							? StandardBasicTypes.CALENDAR_DATE.getName()
							: StandardBasicTypes.DATE.getName();
				}
				default: {
					// java is funny sometimes ;)
					return null;
				}
			}
		}
	}

	public static enum TemporalNature {
		JDK_DATE,
		JDBC_DATE,
		JDBC_TIME,
		JDBC_TIMESTAMP,
		CALENDAR,
		NONE
	}

}
