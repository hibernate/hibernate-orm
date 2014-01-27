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
package org.hibernate.metamodel.internal.source.annotations.attribute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.persistence.FetchType;
import javax.persistence.GenerationType;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.SourceType;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.internal.source.annotations.attribute.type.AttributeTypeResolver;
import org.hibernate.metamodel.internal.source.annotations.attribute.type.CompositeAttributeTypeResolver;
import org.hibernate.metamodel.internal.source.annotations.attribute.type.EnumeratedTypeResolver;
import org.hibernate.metamodel.internal.source.annotations.attribute.type.HibernateTypeResolver;
import org.hibernate.metamodel.internal.source.annotations.attribute.type.LobTypeResolver;
import org.hibernate.metamodel.internal.source.annotations.attribute.type.TemporalTypeResolver;
import org.hibernate.metamodel.internal.source.annotations.entity.EntityBindingContext;
import org.hibernate.metamodel.internal.source.annotations.util.EnumConversionHelper;
import org.hibernate.metamodel.internal.source.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JPADotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.binding.IdentifierGeneratorDefinition;
import org.hibernate.metamodel.spi.source.MappingException;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;

/**
 * Represent a basic attribute (explicitly or implicitly mapped).
 *
 * @author Hardy Ferentschik
 */
public class BasicAttribute extends MappedAttribute {

	/**
	 * The id generator in case this basic attribute represents an simple id. Will be {@code null} in case there
	 * is no explicit id generator or the containing entity does not have a simple id
	 */
	private final IdentifierGeneratorDefinition identifierGeneratorDefinition;

	/**
	 * Is this a versioned property (annotated w/ {@code @Version}).
	 */
	private final boolean isVersioned;


	private final SourceType versionSourceType;

	/**
	 * Is this property lazy loaded (see {@link javax.persistence.Basic}).
	 */
	private boolean isLazy = false;

	/**
	 * Is this property optional  (see {@link javax.persistence.Basic}).
	 */
	private boolean isOptional = true;

	/**
	 * Are this properties generated and when
	 */
	private PropertyGeneration propertyGeneration;
	private boolean isInsertable = true;
	private boolean isUpdatable = true;

	private final String customWriteFragment;
	private final String customReadFragment;
	private AttributeTypeResolver resolver;

	public static BasicAttribute createSimpleAttribute(
			String name,
			Class<?> attributeType,
			Nature attributeNature,
			Map<DotName, List<AnnotationInstance>> annotations,
			String accessType,
			EntityBindingContext context) {
		return new BasicAttribute( name, attributeType, attributeNature, accessType, annotations, context );
	}

	BasicAttribute(String name,
				   Class<?> attributeType,
				   Nature attributeNature,
				   String accessType,
				   Map<DotName, List<AnnotationInstance>> annotations,
				   EntityBindingContext context) {
		super( name, attributeType, attributeNature, accessType, annotations, context );

		AnnotationInstance versionAnnotation = JandexHelper.getSingleAnnotation( annotations, JPADotNames.VERSION );
		isVersioned = versionAnnotation != null;

		if ( isVersioned ) {
			AnnotationInstance sourceAnnotation = JandexHelper.getSingleAnnotation(
					annotations,
					HibernateDotNames.SOURCE
			);
			this.versionSourceType = sourceAnnotation !=null ?
					JandexHelper.getEnumValue( sourceAnnotation, "value", SourceType.class,
							getContext().getServiceRegistry().getService( ClassLoaderService.class ) ) : null;
		}
		else {
			versionSourceType = null;
		}

		if ( isId() ) {
			// an id must be unique and cannot be nullable
			for ( Column columnValue : getColumnValues() ) {
				columnValue.setNullable( false );
			}
			identifierGeneratorDefinition = checkGeneratedValueAnnotation();
		}
		else {
			identifierGeneratorDefinition = null;
		}

		checkBasicAnnotation();
		checkGeneratedAnnotation();
		List<AnnotationInstance> columnTransformerAnnotations = getAllColumnTransformerAnnotations();
		String[] readWrite = createCustomReadWrite( columnTransformerAnnotations );
		this.customReadFragment = readWrite[0];
		this.customWriteFragment = readWrite[1];

	}

	public boolean isVersioned() {
		return isVersioned;
	}

	public boolean isLazy() {
		return isLazy;
	}

	public boolean isOptional() {
		return isOptional;
	}

	public boolean isInsertable() {
		return isInsertable;
	}

	public boolean isUpdatable() {
		return isUpdatable;
	}

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
	public boolean isOptimisticLockable() {
		boolean isOptimisticLockable = super.isOptimisticLockable();
		if ( !isOptimisticLockable ) {
			if ( isId() || isVersioned() ) {
				throw new AnnotationException(
						"@OptimisticLock.exclude=true incompatible with @Id, @EmbeddedId and @Version: "
								+ getRole()
				);
			}
		}
		return isOptimisticLockable;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "SimpleAttribute" );
		sb.append( "{name=" ).append( getRole() );
		return sb.toString();
	}

	private void checkBasicAnnotation() {
		AnnotationInstance basicAnnotation = JandexHelper.getSingleAnnotation( annotations(), JPADotNames.BASIC );
		if ( basicAnnotation != null ) {
			FetchType fetchType = FetchType.LAZY;
			AnnotationValue fetchValue = basicAnnotation.value( "fetch" );
			if ( fetchValue != null ) {
				fetchType = Enum.valueOf( FetchType.class, fetchValue.asEnum() );
			}
			this.isLazy = fetchType == FetchType.LAZY;

			AnnotationValue optionalValue = basicAnnotation.value( "optional" );
			if ( optionalValue != null ) {
				this.isOptional = optionalValue.asBoolean();
			}
		}
	}

	// TODO - there is more todo for updatable and insertable. Checking the @Generated annotation is only one part (HF)
	private void checkGeneratedAnnotation() {
		AnnotationInstance generatedAnnotation = JandexHelper.getSingleAnnotation(
				annotations(),
				HibernateDotNames.GENERATED
		);
		if ( generatedAnnotation != null ) {
			this.isInsertable = false;

			AnnotationValue generationTimeValue = generatedAnnotation.value();
			if ( generationTimeValue != null ) {
				GenerationTime genTime = Enum.valueOf( GenerationTime.class, generationTimeValue.asEnum() );
				if ( GenerationTime.ALWAYS.equals( genTime ) ) {
					this.isUpdatable = false;
					this.propertyGeneration = PropertyGeneration.parse( genTime.toString().toLowerCase() );
				}
			}
		}
		else {
			if ( isId() ) {
				this.isInsertable = true;
				this.isUpdatable = false;
				this.propertyGeneration = PropertyGeneration.INSERT;
			}
		}
	}

	private List<AnnotationInstance> getAllColumnTransformerAnnotations() {
		List<AnnotationInstance> allColumnTransformerAnnotations = new ArrayList<AnnotationInstance>();

		// not quite sure about the usefulness of @ColumnTransformers (HF)
		AnnotationInstance columnTransformersAnnotations = JandexHelper.getSingleAnnotation(
				annotations(),
				HibernateDotNames.COLUMN_TRANSFORMERS
		);
		if ( columnTransformersAnnotations != null ) {
			AnnotationInstance[] annotationInstances = allColumnTransformerAnnotations.get( 0 ).value().asNestedArray();
			allColumnTransformerAnnotations.addAll( Arrays.asList( annotationInstances ) );
		}

		AnnotationInstance columnTransformerAnnotation = JandexHelper.getSingleAnnotation(
				annotations(),
				HibernateDotNames.COLUMN_TRANSFORMER
		);
		if ( columnTransformerAnnotation != null ) {
			allColumnTransformerAnnotations.add( columnTransformerAnnotation );
		}
		return allColumnTransformerAnnotations;
	}

	private String[] createCustomReadWrite(List<AnnotationInstance> columnTransformerAnnotations) {
		String[] readWrite = new String[2];

		boolean alreadyProcessedForColumn = false;
		for ( AnnotationInstance annotationInstance : columnTransformerAnnotations ) {
			String forColumn = annotationInstance.value( "forColumn" ) == null ?
					null : annotationInstance.value( "forColumn" ).asString();
			if ( forColumn != null && !isColumnPresentForTransformer( forColumn ) ) {
				continue;
			}

			if ( alreadyProcessedForColumn ) {
				throw new AnnotationException( "Multiple definition of read/write conditions for column " + getRole() );
			}

			readWrite[0] = annotationInstance.value( "read" ) == null ?
					null : annotationInstance.value( "read" ).asString();
			readWrite[1] = annotationInstance.value( "write" ) == null ?
					null : annotationInstance.value( "write" ).asString();

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

	private IdentifierGeneratorDefinition checkGeneratedValueAnnotation() {
		AnnotationInstance generatedValueAnnotation = JandexHelper.getSingleAnnotation(
				annotations(),
				JPADotNames.GENERATED_VALUE
		);
		if ( generatedValueAnnotation == null ) {
			return null;
		}

		IdentifierGeneratorDefinition generator = null;
		String name = JandexHelper.getValue( generatedValueAnnotation, "generator", String.class,
				getContext().getServiceRegistry().getService( ClassLoaderService.class ) );
		if ( StringHelper.isNotEmpty( name ) ) {
			generator = getContext().findIdGenerator( name );
			if ( generator == null ) {
				throw new MappingException( String.format( "Unable to find named generator %s", getRole() ), getContext().getOrigin() );
			}
		}
		else {
			GenerationType genType = JandexHelper.getEnumValue( generatedValueAnnotation, "strategy", GenerationType.class,
					getContext().getServiceRegistry().getService( ClassLoaderService.class ) );
			String strategy = EnumConversionHelper.generationTypeToGeneratorStrategyName(
					genType,
					getContext().getMetadataImplementor().getOptions().useNewIdentifierGenerators()
			);
			generator = new IdentifierGeneratorDefinition( null, strategy, null );
		}
		return generator;
	}

	@Override
	public AttributeTypeResolver getHibernateTypeResolver() {
		if ( resolver == null ) {
			resolver = getDefaultHibernateTypeResolver();
		}
		return resolver;
	}

	private AttributeTypeResolver getDefaultHibernateTypeResolver() {
		CompositeAttributeTypeResolver resolver = new CompositeAttributeTypeResolver( this );
		resolver.addHibernateTypeResolver( HibernateTypeResolver.createAttributeTypeResolver( this ) );
		resolver.addHibernateTypeResolver( TemporalTypeResolver.createAttributeTypeResolver( this ) );
		resolver.addHibernateTypeResolver( LobTypeResolver.createAttributeTypeResolve( this ) );
		resolver.addHibernateTypeResolver( EnumeratedTypeResolver.createAttributeTypeResolver( this ) );
		return resolver;
	}
}


