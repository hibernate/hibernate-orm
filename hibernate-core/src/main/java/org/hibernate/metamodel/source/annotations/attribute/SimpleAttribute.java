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

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.source.annotations.HibernateDotNames;
import org.hibernate.metamodel.source.annotations.JPADotNames;
import org.hibernate.metamodel.source.annotations.JandexHelper;

/**
 * Represent a mapped attribute (explicitly or implicitly mapped). Also used for synthetic attributes like a
 * discriminator column.
 *
 * @author Hardy Ferentschik
 */
public class SimpleAttribute extends MappedAttribute {
	/**
	 * Is this property an id property (or part thereof).
	 */
	private final boolean isId;

	/**
	 * Is this a versioned property (annotated w/ {@code @Version}.
	 */
	private final boolean isVersioned;

	/**
	 * Whether a change of the property's value triggers a version increment of the entity (in case of optimistic
	 * locking).
	 */
	private final boolean isOptimisticLockable;

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

	/**
	 * Defines the column values (relational values) for this property.
	 */
	private ColumnValues columnValues;

	private final String customWriteFragment;
	private final String customReadFragment;
	private final String checkCondition;

	public static SimpleAttribute createSimpleAttribute(String name, Class<?> attributeType, Map<DotName, List<AnnotationInstance>> annotations, String accessType) {
		return new SimpleAttribute( name, attributeType, accessType, annotations );
	}

	SimpleAttribute(String name, Class<?> attributeType, String accessType, Map<DotName, List<AnnotationInstance>> annotations) {
		super( name, attributeType, accessType, annotations );

		AnnotationInstance idAnnotation = JandexHelper.getSingleAnnotation( annotations, JPADotNames.ID );
		AnnotationInstance embeddedIdAnnotation = JandexHelper.getSingleAnnotation(
				annotations,
				JPADotNames.EMBEDDED_ID
		);
		isId = !( idAnnotation == null && embeddedIdAnnotation == null );

		AnnotationInstance versionAnnotation = JandexHelper.getSingleAnnotation( annotations, JPADotNames.VERSION );
		isVersioned = versionAnnotation != null;

		AnnotationInstance columnAnnotation = JandexHelper.getSingleAnnotation( annotations, JPADotNames.COLUMN );
		columnValues = new ColumnValues( columnAnnotation );

		if ( isId ) {
			// an id must be unique and cannot be nullable
			columnValues.setUnique( true );
			columnValues.setNullable( false );
		}

		this.isOptimisticLockable = checkOptimisticLockAnnotation();

		checkBasicAnnotation();
		checkGeneratedAnnotation();

		String[] readWrite;
		List<AnnotationInstance> columnTransformerAnnotations = getAllColumnTransformerAnnotations();
		readWrite = createCustomReadWrite( columnTransformerAnnotations );
		this.customReadFragment = readWrite[0];
		this.customWriteFragment = readWrite[1];
		this.checkCondition = parseCheckAnnotation();
	}

	public final ColumnValues getColumnValues() {
		return columnValues;
	}

	public boolean isId() {
		return isId;
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

	public boolean isOptimisticLockable() {
		return isOptimisticLockable;
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

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "SimpleAttribute" );
		sb.append( "{name=" ).append( getName() );
		return sb.toString();
	}

	private boolean checkOptimisticLockAnnotation() {
		boolean triggersVersionIncrement = true;
		AnnotationInstance optimisticLockAnnotation = JandexHelper.getSingleAnnotation(
				annotations(),
				HibernateDotNames.OPTIMISTIC_LOCK
		);
		if ( optimisticLockAnnotation != null ) {
			boolean exclude = optimisticLockAnnotation.value( "excluded" ).asBoolean();
			triggersVersionIncrement = !exclude;
		}
		return triggersVersionIncrement;
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
}


