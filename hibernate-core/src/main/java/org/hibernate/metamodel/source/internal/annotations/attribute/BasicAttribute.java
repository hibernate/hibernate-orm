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
package org.hibernate.metamodel.source.internal.annotations.attribute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.persistence.AccessType;
import javax.persistence.FetchType;
import javax.persistence.GenerationType;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.SourceType;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.reflite.spi.MemberDescriptor;
import org.hibernate.metamodel.source.internal.annotations.attribute.type.AttributeTypeResolver;
import org.hibernate.metamodel.source.internal.annotations.attribute.type.AttributeTypeResolverComposition;
import org.hibernate.metamodel.source.internal.annotations.attribute.type.EnumeratedTypeResolver;
import org.hibernate.metamodel.source.internal.annotations.attribute.type.HibernateTypeResolver;
import org.hibernate.metamodel.source.internal.annotations.attribute.type.LobTypeResolver;
import org.hibernate.metamodel.source.internal.annotations.attribute.type.TemporalTypeResolver;
import org.hibernate.metamodel.source.internal.annotations.entity.ManagedTypeMetadata;
import org.hibernate.metamodel.source.internal.annotations.util.EnumConversionHelper;
import org.hibernate.metamodel.source.internal.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.AttributePath;
import org.hibernate.metamodel.spi.AttributeRole;
import org.hibernate.metamodel.spi.NaturalIdMutability;
import org.hibernate.metamodel.spi.binding.IdentifierGeneratorDefinition;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

/**
 * Represents a singular persistent attribute that is non-composite and non-association.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public class BasicAttribute extends AbstractSingularAttribute {
	private final SourceType versionSourceType;
	private final IdentifierGeneratorDefinition identifierGeneratorDefinition;

	private boolean isLazy;
	private boolean isOptional;

	private final PropertyGeneration propertyGeneration;
	private final ColumnInclusion insertability;
	private final ColumnInclusion updateability;

	private final String customWriteFragment;
	private final String customReadFragment;

	private AttributeTypeResolver resolver;

	public BasicAttribute(
			ManagedTypeMetadata container,
			String attributeName,
			AttributePath attributePath,
			AttributeRole attributeRole,
			MemberDescriptor backingMember,
			AccessType accessType,
			String accessStrategy) {
		super(
				container,
				attributeName,
				attributePath,
				attributeRole,
				backingMember,
				Nature.BASIC,
				accessType,
				accessStrategy
		);

		this.insertability = new ColumnInclusion( container.canAttributesBeInsertable() );
		this.updateability = new ColumnInclusion( container.canAttributesBeUpdatable() );

		// a basic attribute can be a version attribute
		this.versionSourceType = isVersion() ? extractVersionSourceType( backingMember ) : null;

		if ( isId() ) {
			// an id must be unique and cannot be nullable
			for ( Column columnValue : getColumnValues() ) {
				columnValue.setNullable( false );
			}
			updateability.disable();
			identifierGeneratorDefinition = extractIdentifierGeneratorDefinition( backingMember );
		}
		else {
			identifierGeneratorDefinition = null;
		}

		// @Basic
		final AnnotationInstance basicAnnotation = backingMember.getAnnotations().get( JPADotNames.BASIC );
		if ( basicAnnotation == null ) {
			isLazy = false;
			isOptional = true;
		}
		else {
			FetchType fetchType = FetchType.EAGER;
			final AnnotationValue fetchValue = basicAnnotation.value( "fetch" );
			if ( fetchValue != null ) {
				fetchType = Enum.valueOf( FetchType.class, fetchValue.asEnum() );
			}
			this.isLazy = fetchType == FetchType.LAZY;

			boolean optional = true;
			final AnnotationValue optionalValue = basicAnnotation.value( "optional" );
			if ( optionalValue != null ) {
				optional = optionalValue.asBoolean();
			}
			this.isOptional = optional;
		}

		// @Generated
		// todo : hook in the new generation stuff
		final AnnotationInstance generatedAnnotation = backingMember.getAnnotations().get( HibernateDotNames.GENERATED );
		if ( generatedAnnotation == null ) {
			if ( isId() ) {
				this.updateability.disable();
				this.propertyGeneration = PropertyGeneration.INSERT;
			}
			else {
				this.propertyGeneration = PropertyGeneration.ALWAYS;
			}
		}
		else {
			this.insertability.disable();

			PropertyGeneration propertyGeneration = PropertyGeneration.ALWAYS;
			AnnotationValue generationTimeValue = generatedAnnotation.value();
			if ( generationTimeValue != null ) {
				GenerationTime genTime = Enum.valueOf( GenerationTime.class, generationTimeValue.asEnum() );
				if ( GenerationTime.ALWAYS.equals( genTime ) ) {
					this.updateability.disable();
					propertyGeneration = PropertyGeneration.parse( genTime.toString().toLowerCase() );
				}
			}
			this.propertyGeneration = propertyGeneration;
		}

		if ( getNaturalIdMutability() == NaturalIdMutability.IMMUTABLE ) {
			this.updateability.disable();
		}

		List<AnnotationInstance> columnTransformerAnnotations = collectColumnTransformerAnnotations( backingMember );
		String[] readWrite = createCustomReadWrite( columnTransformerAnnotations );
		this.customReadFragment = readWrite[0];
		this.customWriteFragment = readWrite[1];
	}

	@Override
	protected void validatePresenceOfIdAnnotation() {
		// ok here
	}

	@Override
	protected void validatePresenceOfEmbeddedIdAnnotation() {
		// ok here
	}

	@Override
	protected void validatePresenceOfVersionAnnotation() {
		// ok here
	}

	@Override
	protected void validatePresenceOfNaturalIdAnnotation() {
		// ok here
	}

	@Override
	protected void validatePresenceOfColumnAnnotation() {
		// ok here
	}

	@Override
	protected void validatePresenceOfColumnsAnnotation() {
		// ok here
	}

	private SourceType extractVersionSourceType(MemberDescriptor backingMember) {
		final AnnotationInstance sourceAnnotation = backingMember.getAnnotations().get( HibernateDotNames.SOURCE );
		if ( sourceAnnotation == null ) {
			return null;
		}

		return JandexHelper.getEnumValue(
				sourceAnnotation,
				"value",
				SourceType.class,
				getContext().getServiceRegistry().getService( ClassLoaderService.class )
		);
	}

	private IdentifierGeneratorDefinition extractIdentifierGeneratorDefinition(MemberDescriptor backingMember) {
		final AnnotationInstance generatedValueAnnotation = backingMember.getAnnotations().get( JPADotNames.GENERATED_VALUE );
		if ( generatedValueAnnotation == null ) {
			return null;
		}

		IdentifierGeneratorDefinition generator = null;

		final String generatorName = JandexHelper.getValue(
				generatedValueAnnotation,
				"generator",
				String.class,
				getContext().getServiceRegistry().getService( ClassLoaderService.class )
		);
		if ( StringHelper.isNotEmpty( generatorName ) ) {
			generator = getContext().findIdGenerator( generatorName );
			if ( generator == null ) {
				throw getContext().makeMappingException(
						String.format( "Unable to find named generator [%s] for %s", generatorName, getRole() )
				);
			}
		}
		else {
			final GenerationType genType = JandexHelper.getEnumValue(
					generatedValueAnnotation,
					"strategy",
					GenerationType.class,
					getContext().getServiceRegistry().getService( ClassLoaderService.class )
			);
			final String strategy = EnumConversionHelper.generationTypeToGeneratorStrategyName(
					genType,
					getContext().getBuildingOptions().isUseNewIdentifierGenerators()
			);
			generator = new IdentifierGeneratorDefinition( null, strategy, null );
		}
		return generator;
	}

	private List<AnnotationInstance> collectColumnTransformerAnnotations(MemberDescriptor backingMember) {
		List<AnnotationInstance> allColumnTransformerAnnotations = new ArrayList<AnnotationInstance>();

		final AnnotationInstance columnTransformerAnnotation = backingMember.getAnnotations().get(
				HibernateDotNames.COLUMN_TRANSFORMER
		);
		final AnnotationInstance columnTransformersAnnotations = backingMember.getAnnotations().get(
				HibernateDotNames.COLUMN_TRANSFORMERS
		);

		if ( columnTransformerAnnotation != null && columnTransformersAnnotations != null ) {
			throw getContext().makeMappingException(
					"Should not mix @ColumnTransformer and @ColumnTransformers annotations " +
							"on same attribute : " + backingMember.toString()
			);
		}

		if ( columnTransformersAnnotations != null ) {
			AnnotationInstance[] annotationInstances = allColumnTransformerAnnotations.get( 0 ).value().asNestedArray();
			allColumnTransformerAnnotations.addAll( Arrays.asList( annotationInstances ) );
		}

		if ( columnTransformerAnnotation != null ) {
			allColumnTransformerAnnotations.add( columnTransformerAnnotation );
		}
		return allColumnTransformerAnnotations;
	}

	private String[] createCustomReadWrite(List<AnnotationInstance> columnTransformerAnnotations) {
		String[] readWrite = new String[2];

		boolean alreadyProcessedForColumn = false;
		for ( AnnotationInstance annotationInstance : columnTransformerAnnotations ) {
			String forColumn = annotationInstance.value( "forColumn" ) == null
					? null
					: annotationInstance.value( "forColumn" ).asString();
			if ( forColumn != null && !isColumnPresentForTransformer( forColumn ) ) {
				continue;
			}

			if ( alreadyProcessedForColumn ) {
				throw new AnnotationException( "Multiple definition of read/write conditions for column " + getRole() );
			}

			readWrite[0] = annotationInstance.value( "read" ) == null
					? null
					: annotationInstance.value( "read" ).asString();
			readWrite[1] = annotationInstance.value( "write" ) == null
					? null :
					annotationInstance.value( "write" ).asString();

			alreadyProcessedForColumn = true;
		}
		return readWrite;
	}

	private boolean isColumnPresentForTransformer(final String forColumn) {
		assert forColumn != null;
		List<Column> columns = getColumnValues();
		for ( final Column column : columns ) {
			if ( forColumn.equals( column.getName() ) ) {
				return true;
			}
		}
		return forColumn.equals( getName() );
	}

	@Override
	public boolean isLazy() {
		return isLazy;
	}

	@Override
	public boolean isOptional() {
		return isOptional;
	}

	@Override
	public boolean isInsertable() {
		return insertability.shouldInclude();
	}

	@Override
	public boolean isUpdatable() {
		return updateability.shouldInclude();
	}

	@Override
	public PropertyGeneration getPropertyGeneration() {
		return propertyGeneration;
	}

	public String getCustomWriteFragment() {
		return customWriteFragment;
	}

	public String getCustomReadFragment() {
		return customReadFragment;
	}

	public IdentifierGeneratorDefinition getIdentifierGeneratorDefinition() {
		return identifierGeneratorDefinition;
	}

	public SourceType getVersionSourceType() {
		return versionSourceType;
	}

	@Override
	public AttributeTypeResolver getHibernateTypeResolver() {
		if ( resolver == null ) {
			resolver = buildTypeResolver();
		}
		return resolver;
	}

	private AttributeTypeResolver buildTypeResolver() {
		return new AttributeTypeResolverComposition(
				getBackingMember().getType().getErasedType(),
				getContext(),
				HibernateTypeResolver.createAttributeTypeResolver( this ),
				TemporalTypeResolver.createAttributeTypeResolver( this ),
				LobTypeResolver.createAttributeTypeResolve( this ),
				EnumeratedTypeResolver.createAttributeTypeResolver( this )
		);
	}

	@Override
	public String toString() {
		return "BasicAttribute{name=" + getBackingMember().toString() + '}';
	}
}
