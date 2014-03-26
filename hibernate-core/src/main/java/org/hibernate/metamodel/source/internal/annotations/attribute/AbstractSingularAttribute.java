/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.internal.annotations.attribute;

import javax.persistence.AccessType;

import org.hibernate.AnnotationException;
import org.hibernate.metamodel.reflite.spi.MemberDescriptor;
import org.hibernate.metamodel.source.internal.AttributeConversionInfo;
import org.hibernate.metamodel.source.internal.annotations.entity.ManagedTypeMetadata;
import org.hibernate.metamodel.source.internal.annotations.util.ConverterAndOverridesHelper;
import org.hibernate.metamodel.source.internal.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.metamodel.spi.AttributePath;
import org.hibernate.metamodel.spi.AttributeRole;
import org.hibernate.metamodel.spi.NaturalIdMutability;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSingularAttribute
		extends AbstractPersistentAttribute
		implements SingularAttribute {
	private static final Logger log = Logger.getLogger( AbstractSingularAttribute.class );

	protected AbstractSingularAttribute(
			ManagedTypeMetadata container,
			String attributeName,
			AttributePath attributePath,
			AttributeRole attributeRole,
			MemberDescriptor backingMember,
			Nature nature,
			AccessType accessType,
			String accessorStrategy) {
		super( container, attributeName, attributePath, attributeRole, backingMember, nature, accessType, accessorStrategy );

		if ( backingMember.getAnnotations().containsKey( HibernateDotNames.IMMUTABLE ) ) {
			throw new AnnotationException( "@Immutable can be used on entities or collections, not "
					+ attributeRole.getFullPath() );
		}
		
		ConverterAndOverridesHelper.INSTANCE.processConverters(
				getPath(),
				getNature(),
				backingMember,
				container,
				getContext()
		);
		ConverterAndOverridesHelper.INSTANCE.processAttributeOverrides(
				getPath(),
				backingMember,
				container,
				getContext()
		);
		ConverterAndOverridesHelper.INSTANCE.processAssociationOverrides(
				getPath(),
				backingMember,
				container,
				getContext()
		);
	}

	protected AttributeConversionInfo validateConversionInfo(AttributeConversionInfo conversionInfo) {
		// NOTE we cant really throw exceptions here atm because we do not know if
		// 		the converter was explicitly requested or if an auto-apply converter
		//		was returned.  So we simply log a warning and circumvent the conversion

		// todo : regarding ^^, on second thought its likely better if this scaffolding just support locally defined converters
		//		then it is ok to throw then exceptions
		//		the idea being that binder would apply auto apply converters if needed

		// disabled is always allowed
		if ( !conversionInfo.isConversionEnabled() ) {
			return conversionInfo;
		}

		// otherwise use of the converter is ok, as long as...
		//		1) the attribute is not an id
		if ( isId() ) {
			log.warnf(
					"AttributeConverter [%s] cannot be applied to given attribute [%s] as it is an Id (section 3.8)",
					conversionInfo.getConverterTypeDescriptor().getName(),
					getBackingMember().toString()
			);
			return null;
		}

		//		2) the attribute is not a version
		if ( isVersion() ) {
			log.warnf(
					"AttributeConverter [%s] cannot be applied to given attribute [%s] as it is a Version (section 3.8)",
					conversionInfo.getConverterTypeDescriptor().getName(),
					getBackingMember().toString()
			);
			return null;
		}

		//		3) the attribute is not an association
		if ( getNature() == Nature.MANY_TO_ONE || getNature() == Nature.ONE_TO_ONE ) {
			log.warnf(
					"AttributeConverter [%s] cannot be applied to given attribute [%s] as it is an association (section 3.8)",
					conversionInfo.getConverterTypeDescriptor().getName(),
					getBackingMember().toString()
			);
			return null;
		}

		//		4) the attribute is not an embedded
		if ( getNature() == Nature.EMBEDDED || getNature() == Nature.EMBEDDED_ID ) {
			log.warnf(
					"AttributeConverter [%s] cannot be applied to given attribute [%s] as it is an Embeddable (section 3.8)",
					conversionInfo.getConverterTypeDescriptor().getName(),
					getBackingMember().toString()
			);
			return null;
		}

		//		5) the attribute cannot have explicit "conversion" annotations such as
		//			@Temporal or @Enumerated
		if ( getBackingMember().getAnnotations().containsKey( JPADotNames.TEMPORAL ) ) {
			log.warnf(
					"AttributeConverter [%s] cannot be applied to given attribute [%s] as it is annotated @Temporal (section 3.8)",
					conversionInfo.getConverterTypeDescriptor().getName(),
					getBackingMember().toString()
			);
			return null;
		}
		if ( getBackingMember().getAnnotations().containsKey( JPADotNames.ENUMERATED ) ) {
			log.warnf(
					"AttributeConverter [%s] cannot be applied to given attribute [%s] as it is annotated @Enumerated (section 3.8)",
					conversionInfo.getConverterTypeDescriptor().getName(),
					getBackingMember().toString()
			);
			return null;
		}


		return conversionInfo;
	}

	@Override
	public AttributeConversionInfo getConversionInfo() {
		return getContainer().locateConversionInfo( getPath() );
	}

	@Override
	public boolean isId() {
		return super.isId();
	}

	@Override
	public boolean isVersion() {
		return super.isVersion();
	}

	@Override
	public NaturalIdMutability getNaturalIdMutability() {
		return super.getNaturalIdMutability();
	}
}
