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
package org.hibernate.metamodel.source.annotations.attribute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.persistence.FetchType;
import javax.persistence.GenerationType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.binding.IdGenerator;
import org.hibernate.metamodel.source.MappingException;
import org.hibernate.metamodel.source.annotations.EnumConversionHelper;
import org.hibernate.metamodel.source.annotations.HibernateDotNames;
import org.hibernate.metamodel.source.annotations.JPADotNames;
import org.hibernate.metamodel.source.annotations.JandexHelper;
import org.hibernate.metamodel.source.annotations.attribute.type.AttributeTypeResolver;
import org.hibernate.metamodel.source.annotations.attribute.type.AttributeTypeResolverImpl;
import org.hibernate.metamodel.source.annotations.attribute.type.CompositeAttributeTypeResolver;
import org.hibernate.metamodel.source.annotations.attribute.type.EnumeratedTypeResolver;
import org.hibernate.metamodel.source.annotations.attribute.type.LobTypeResolver;
import org.hibernate.metamodel.source.annotations.attribute.type.TemporalTypeResolver;
import org.hibernate.metamodel.source.annotations.entity.EntityBindingContext;

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
	private final IdGenerator idGenerator;

	/**
	 * Is this a versioned property (annotated w/ {@code @Version}.
	 */
	private final boolean isVersioned;

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
	private final String checkCondition;
	private AttributeTypeResolver resolver;

	public static BasicAttribute createSimpleAttribute(String name,
													   Class<?> attributeType,
													   Map<DotName, List<AnnotationInstance>> annotations,
													   String accessType,
													   EntityBindingContext context) {
		return new BasicAttribute( name, attributeType, accessType, annotations, context );
	}

	BasicAttribute(String name,
				   Class<?> attributeType,
				   String accessType,
				   Map<DotName, List<AnnotationInstance>> annotations,
				   EntityBindingContext context) {
		super( name, attributeType, accessType, annotations, context );

		AnnotationInstance versionAnnotation = JandexHelper.getSingleAnnotation( annotations, JPADotNames.VERSION );
		isVersioned = versionAnnotation != null;

		if ( isId() ) {
			// an id must be unique and cannot be nullable
			getColumnValues().setUnique( true );
			getColumnValues().setNullable( false );
			idGenerator = checkGeneratedValueAnnotation();
		}
		else {
			idGenerator = null;
		}

		checkBasicAnnotation();
		checkGeneratedAnnotation();

		List<AnnotationInstance> columnTransformerAnnotations = getAllColumnTransformerAnnotations();
		String[] readWrite = createCustomReadWrite( columnTransformerAnnotations );
		this.customReadFragment = readWrite[0];
		this.customWriteFragment = readWrite[1];
		this.checkCondition = parseCheckAnnotation();
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

	public String getCheckCondition() {
		return checkCondition;
	}

	public IdGenerator getIdGenerator() {
		return idGenerator;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "SimpleAttribute" );
		sb.append( "{name=" ).append( getName() );
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

			if ( forColumn != null && !forColumn.equals( getName() ) ) {
				continue;
			}

			if ( alreadyProcessedForColumn ) {
				throw new AnnotationException( "Multiple definition of read/write conditions for column " + getName() );
			}

			readWrite[0] = annotationInstance.value( "read" ) == null ?
					null : annotationInstance.value( "read" ).asString();
			readWrite[1] = annotationInstance.value( "write" ) == null ?
					null : annotationInstance.value( "write" ).asString();

			alreadyProcessedForColumn = true;
		}
		return readWrite;
	}

	private String parseCheckAnnotation() {
		String checkCondition = null;
		AnnotationInstance checkAnnotation = JandexHelper.getSingleAnnotation( annotations(), HibernateDotNames.CHECK );
		if ( checkAnnotation != null ) {
			checkCondition = checkAnnotation.value( "constraints" ).toString();
		}
		return checkCondition;
	}

	private IdGenerator checkGeneratedValueAnnotation() {
		IdGenerator generator = null;
		AnnotationInstance generatedValueAnnotation = JandexHelper.getSingleAnnotation(
				annotations(),
				JPADotNames.GENERATED_VALUE
		);
		if ( generatedValueAnnotation != null ) {
			String name = JandexHelper.getValue( generatedValueAnnotation, "generator", String.class );
			if ( StringHelper.isNotEmpty( name ) ) {
				generator = getContext().getMetadataImplementor().getIdGenerator( name );
				if ( generator == null ) {
					throw new MappingException( String.format( "Unable to find named generator %s", name ), null );
				}
			}
			else {
				GenerationType genType = JandexHelper.getEnumValue(
						generatedValueAnnotation,
						"strategy",
						GenerationType.class
				);
				String strategy = EnumConversionHelper.generationTypeToGeneratorStrategyName(
						genType,
						getContext().getMetadataImplementor().getOptions().useNewIdentifierGenerators()
				);
				generator = new IdGenerator( null, strategy, null );
			}
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
		CompositeAttributeTypeResolver resolver = new CompositeAttributeTypeResolver(
				new AttributeTypeResolverImpl(
						this
				)
		);
		resolver.addHibernateTypeResolver( new TemporalTypeResolver( this ) );
		resolver.addHibernateTypeResolver( new LobTypeResolver( this ) );
		resolver.addHibernateTypeResolver( new EnumeratedTypeResolver( this ) );
		return resolver;
	}
}


