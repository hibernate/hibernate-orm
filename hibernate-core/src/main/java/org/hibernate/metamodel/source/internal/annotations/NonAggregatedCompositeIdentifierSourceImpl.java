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
package org.hibernate.metamodel.source.internal.annotations;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hibernate.id.EntityIdentifierNature;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.reflite.spi.MemberDescriptor;
import org.hibernate.metamodel.source.internal.AttributeConversionInfo;
import org.hibernate.metamodel.source.internal.annotations.attribute.AssociationOverride;
import org.hibernate.metamodel.source.internal.annotations.attribute.AttributeOverride;
import org.hibernate.metamodel.source.internal.annotations.attribute.EmbeddedContainer;
import org.hibernate.metamodel.source.internal.annotations.entity.EmbeddableTypeMetadata;
import org.hibernate.metamodel.source.internal.annotations.entity.EntityBindingContext;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.metamodel.source.spi.AttributeSource;
import org.hibernate.metamodel.source.spi.EmbeddableSource;
import org.hibernate.metamodel.source.spi.NonAggregatedCompositeIdentifierSource;
import org.hibernate.metamodel.source.spi.SingularAttributeSource;
import org.hibernate.metamodel.source.spi.ToolingHintSource;
import org.hibernate.metamodel.spi.AttributePath;
import org.hibernate.metamodel.spi.AttributeRole;
import org.hibernate.metamodel.spi.NaturalIdMutability;
import org.hibernate.metamodel.spi.binding.IdentifierGeneratorDefinition;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

/**
* @author Steve Ebersole
*/
class NonAggregatedCompositeIdentifierSourceImpl
		extends AbstractIdentifierSource
		implements NonAggregatedCompositeIdentifierSource {
	private static final Logger log = Logger.getLogger( NonAggregatedCompositeIdentifierSourceImpl.class );

	private final List<SingularAttributeSource> idAttributeSources;
	private final IdClassSource idClassSource;

	public NonAggregatedCompositeIdentifierSourceImpl(RootEntitySourceImpl rootEntitySource) {
		super( rootEntitySource );

		this.idAttributeSources = rootEntitySource.getIdentifierAttributes();

		final JavaTypeDescriptor idClassDescriptor = resolveIdClassDescriptor();
		if ( idClassDescriptor == null ) {
			// todo : map this a MessageLogger message
			log.warnf(
					"Encountered non-aggregated identifier with no IdClass specified; while this is supported, " +
							"its use should be considered deprecated"
			);
			this.idClassSource = null;
		}
		else {
			this.idClassSource = new IdClassSource( rootEntitySource, idClassDescriptor );
		}






	}

	private JavaTypeDescriptor resolveIdClassDescriptor() {
		final AnnotationInstance idClassAnnotation = rootEntitySource().getEntityClass()
				.getJavaTypeDescriptor()
				.findTypeAnnotation( JPADotNames.ID_CLASS );

		if ( idClassAnnotation == null ) {
			return null;
		}

		if ( idClassAnnotation.value() == null ) {
			return null;
		}

		return rootEntitySource().getLocalBindingContext().getJavaTypeDescriptorRepository().getType(
				DotName.createSimple( idClassAnnotation.value().asString() )
		);
	}

	@Override
	public List<SingularAttributeSource> getAttributeSourcesMakingUpIdentifier() {
		return idAttributeSources;
	}

	@Override
	public EmbeddableSource getIdClassSource() {
		return idClassSource;
	}

	@Override
	public IdentifierGeneratorDefinition getIndividualAttributeIdGenerator(String identifierAttributeName) {
		// for now, return null.  this is that stupid specj bs
		return null;
	}

	@Override
	public IdentifierGeneratorDefinition getIdentifierGeneratorDescriptor() {
		// annotations do not currently allow generators to be attached to composite identifiers as a whole
		return null;
	}

	@Override
	public EntityIdentifierNature getNature() {
		return EntityIdentifierNature.NON_AGGREGATED_COMPOSITE;
	}

	@Override
	public String getUnsavedValue() {
		return null;
	}

	@Override
	public Collection<? extends ToolingHintSource> getToolingHintSources() {
		// not relevant for annotations
		// todo : however, it is relevant for mocked annotations comeing from XML!!!
		return Collections.emptySet();
	}

	private class IdClassSource implements EmbeddableSource, EmbeddedContainer {
		private final JavaTypeDescriptor idClassDescriptor;
		private final EmbeddableTypeMetadata idClassTypeMetadata;

		private final AttributeRole attributeRoleBase;
		private final AttributePath attributePathBase;

		private List<AttributeSource> attributeSources;

		private IdClassSource(RootEntitySourceImpl rootEntitySource, JavaTypeDescriptor idClassDescriptor) {
			this.idClassDescriptor = idClassDescriptor;

			this.attributeRoleBase = rootEntitySource.getAttributeRoleBase().append( "<IdClass>" );
			this.attributePathBase = rootEntitySource.getAttributePathBase().append( "<IdClass>" );

			final AnnotationAttributeSource firstIdAttribute =
					(AnnotationAttributeSource) rootEntitySource.getIdentifierAttributes().get( 0 );

			this.idClassTypeMetadata = new EmbeddableTypeMetadata(
					idClassDescriptor,
					this,
					attributeRoleBase,
					attributePathBase,
					firstIdAttribute.getAnnotatedAttribute().getAccessType(),
					null,
					rootEntitySource.getLocalBindingContext()
			);

			// todo : locate MapsId annotations and build a specialized AttributeBuilder

			this.attributeSources = SourceHelper.buildAttributeSources(
					idClassTypeMetadata,
					SourceHelper.IdentifierPathAttributeBuilder.INSTANCE
			);

			if ( log.isDebugEnabled() ) {
				String attributeDescriptors = null;
				for ( AttributeSource attributeSource : attributeSources ) {
					if ( attributeDescriptors == null ) {
						attributeDescriptors = attributeSource.getName();
					}
					else {
						attributeDescriptors += ", " + attributeSource.getName();
					}
				}
				log.debugf(
						"Built IdClassSource : %s : %s",
						idClassTypeMetadata.getJavaTypeDescriptor().getName(),
						attributeDescriptors
				);
			}

			// todo : validate the IdClass attributes against the entity's id attributes

			// todo : we need similar (MapsId, validation) in the EmbeddedId case too.
		}

		@Override
		public JavaTypeDescriptor getTypeDescriptor() {
			return idClassDescriptor;
		}

		@Override
		public String getParentReferenceAttributeName() {
			return null;
		}

		@Override
		public String getExplicitTuplizerClassName() {
			return idClassTypeMetadata.getCustomTuplizerClassName();
		}

		@Override
		public AttributePath getAttributePathBase() {
			return attributePathBase;
		}

		@Override
		public AttributeRole getAttributeRoleBase() {
			return attributeRoleBase;
		}

		@Override
		public List<AttributeSource> attributeSources() {
			return attributeSources;
		}

		@Override
		public EntityBindingContext getLocalBindingContext() {
			return idClassTypeMetadata.getLocalBindingContext();
		}

		// EmbeddedContainer impl (most of which we don't care about here

		@Override
		public MemberDescriptor getBackingMember() {
			return null;
		}

		@Override
		public AttributeConversionInfo locateConversionInfo(AttributePath attributePath) {
			return null;
		}

		@Override
		public AttributeOverride locateAttributeOverride(AttributePath attributePath) {
			return null;
		}

		@Override
		public AssociationOverride locateAssociationOverride(
				AttributePath attributePath) {
			return null;
		}

		@Override
		public NaturalIdMutability getContainerNaturalIdMutability() {
			return null;
		}

		@Override
		public boolean getContainerOptionality() {
			return false;
		}

		@Override
		public boolean getContainerUpdatability() {
			return false;
		}

		@Override
		public boolean getContainerInsertability() {
			return false;
		}

		@Override
		public void registerConverter(
				AttributePath attributePath, AttributeConversionInfo conversionInfo) {

		}

		@Override
		public void registerAttributeOverride(
				AttributePath attributePath, AttributeOverride override) {

		}

		@Override
		public void registerAssociationOverride(
				AttributePath attributePath, AssociationOverride override) {

		}
	}


}
