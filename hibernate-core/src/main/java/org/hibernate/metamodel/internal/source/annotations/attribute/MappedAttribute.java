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

import org.hibernate.AnnotationException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.internal.source.annotations.attribute.type.AttributeTypeResolver;
import org.hibernate.metamodel.internal.source.annotations.entity.EntityBindingContext;
import org.hibernate.metamodel.internal.source.annotations.util.AnnotationParserHelper;
import org.hibernate.metamodel.internal.source.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JPADotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

/**
 * Base class for the different types of mapped attributes
 *
 * @author Hardy Ferentschik
 */
public abstract class MappedAttribute implements Comparable<MappedAttribute> {
	/**
	 * Annotations defined on the attribute, keyed against the annotation dot name.
	 */
	private final Map<DotName, List<AnnotationInstance>> annotations;

	/**
	 * The property name.
	 */
	private final String name;

	/**
	 * The java type of the attribute
	 */
	private final Class<?> attributeType;

	/**
	 * The nature of the attribute
	 */
	private final Nature attributeNature;

	/**
	 * The access type for this property. At the moment this is either 'field' or 'property', but Hibernate
	 * also allows custom named accessors (see {@link org.hibernate.property.PropertyAccessorFactory}).
	 */
	private final String accessType;

	/**
	 * Defines the column values (relational values) for this property. A mapped property can refer to multiple
	 * column values in case of components or join columns etc
	 */
	private final List<Column> columnValues = new ArrayList<Column>();

	/**
	 * Is this a formula property ( annotated w/ {@code Formula}).
	 */
	private final FormulaValue formulaValue;


	/**
	 * Is this property an id property (or part thereof).
	 */
	private final boolean isId;

	/**
	 * Is this property a natural id property and what's the mutability it is.
	 */
	private SingularAttributeBinding.NaturalIdMutability naturalIdMutability;

	/**
	 * Whether a change of the property's value triggers a version increment of the entity (in case of optimistic
	 * locking).
	 */
	private final boolean isOptimisticLockable;

	/**
	 * Contains the SQL check condition specified via {@link org.hibernate.annotations.Check} or null if no annotation
	 * is specified.
	 */
	private final String checkCondition;

	/**
	 * FQN of the attribute.
	 */
	private final String role;

	/**
	 * The binding context
	 */
	private final EntityBindingContext context;

	MappedAttribute(String name,
					Class<?> attributeType,
					Nature attributeNature,
					String accessType,
					Map<DotName, List<AnnotationInstance>> annotations,
					EntityBindingContext context
	) {
		this.context = context;
		this.annotations = annotations;
		this.name = name;
		this.attributeType = attributeType;
		this.attributeNature = attributeNature;
		this.accessType = accessType;

		//if this attribute has either @Id or @EmbeddedId, then it is an id attribute
		AnnotationInstance idAnnotation = JandexHelper.getSingleAnnotation( annotations, JPADotNames.ID );
		AnnotationInstance embeddedIdAnnotation = JandexHelper.getSingleAnnotation(
				annotations,
				JPADotNames.EMBEDDED_ID
		);
		this.isId = ( idAnnotation != null || embeddedIdAnnotation != null );

		this.isOptimisticLockable = checkOptimisticLockAnnotation();
		this.checkCondition = checkCheckAnnotation();
		this.naturalIdMutability = AnnotationParserHelper.checkNaturalId( annotations );
		this.role = context.getOrigin().getName() + "#" + name;
		checkColumnAnnotations( annotations );
		this.formulaValue = checkFormulaValueAnnotation();

	}

	public String getName() {
		return name;
	}

	public String getRole(){
		return role;
	}

	public final Class<?> getAttributeType() {
		return attributeType;
	}

	public String getAccessType() {
		return accessType;
	}

	public EntityBindingContext getContext() {
		return context;
	}

	public Map<DotName, List<AnnotationInstance>> annotations() {
		return annotations;
	}

	public List<Column> getColumnValues() {
		return columnValues;
	}

	public boolean isId() {
		return isId;
	}

	public boolean isOptimisticLockable() {
		return isOptimisticLockable;
	}

	public SingularAttributeBinding.NaturalIdMutability getNaturalIdMutability() {
		return naturalIdMutability;
	}

	public void setNaturalIdMutability(SingularAttributeBinding.NaturalIdMutability naturalIdMutability) {
		this.naturalIdMutability = naturalIdMutability;
	}

	public Nature getNature() {
		return attributeNature;
	}

	public String getCheckCondition() {
		return checkCondition;
	}

	public FormulaValue getFormulaValue() {
		return formulaValue;
	}

	@Override
	public int compareTo(MappedAttribute mappedProperty) {
		return name.compareTo( mappedProperty.getName() );
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "MappedAttribute" );
		sb.append( "{name='" ).append( getRole() ).append( '\'' );
		sb.append( '}' );
		return sb.toString();
	}

	public abstract AttributeTypeResolver getHibernateTypeResolver();

	public abstract boolean isLazy();

	public abstract boolean isOptional();

	public abstract boolean isInsertable();

	public abstract boolean isUpdatable();

	public abstract PropertyGeneration getPropertyGeneration();

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

	private FormulaValue checkFormulaValueAnnotation() {
		AnnotationInstance formulaAnnotation = JandexHelper.getSingleAnnotation(
				annotations(),
				HibernateDotNames.FORMULA
		);
		if ( formulaAnnotation != null ) {
			if ( !getColumnValues().isEmpty() ) {
				throw new AnnotationException( "Can't having both @Formula and @Column on same attribute : " + getRole() );
			}
			final String expression = JandexHelper.getValue( formulaAnnotation, "value", String.class,
					context.getServiceRegistry().getService( ClassLoaderService.class ));
			if ( StringHelper.isEmpty( expression ) ) {
				throw new AnnotationException(
						String.format(
								"Formula expression defined on %s can't be empty string",
								getRole()
						)
				);
			}
			return new FormulaValue( null, expression );
		}
		return null;
	}
	private void checkColumnAnnotations(Map<DotName, List<AnnotationInstance>> annotations) {
		// single @Column
		AnnotationInstance columnAnnotation = JandexHelper.getSingleAnnotation(
				annotations,
				JPADotNames.COLUMN
		);
		if ( columnAnnotation != null ) {
			checkWrongColumnAnnotationLocation();
			columnValues.add( new Column( columnAnnotation ) );
		}

		// @org.hibernate.annotations.Columns
		AnnotationInstance columnsAnnotation = JandexHelper.getSingleAnnotation(
				annotations,
				HibernateDotNames.COLUMNS
		);
		if ( columnsAnnotation != null ) {
			checkWrongColumnAnnotationLocation();
			List<AnnotationInstance> columnsList = Arrays.asList(
					JandexHelper.getValue( columnsAnnotation, "columns", AnnotationInstance[].class,
							context.getServiceRegistry().getService( ClassLoaderService.class ) )
			);
			for ( AnnotationInstance annotation : columnsList ) {
				columnValues.add( new Column( annotation ) );
			}
		}
	}

	private void checkWrongColumnAnnotationLocation() {
		if ( getNature() == Nature.MANY_TO_ONE || getNature() == Nature.ONE_TO_ONE ) {
			throw getContext().makeMappingException(
					"@Column(s) not allowed on a " + getNature() + " property: " + getRole()
			);
		}
	}

	private String checkCheckAnnotation() {
		final AnnotationInstance checkAnnotation = JandexHelper.getSingleAnnotation( annotations(), HibernateDotNames.CHECK );
		return checkAnnotation != null ? checkAnnotation.value( "constraints" ).toString() : null;
	}

	/**
	 * An enum defining the type of a mapped attribute.
	 */
	public static enum Nature {
		BASIC( JPADotNames.BASIC ),
		ONE_TO_ONE( JPADotNames.ONE_TO_ONE ),
		ONE_TO_MANY( JPADotNames.ONE_TO_MANY ),
		MANY_TO_ONE( JPADotNames.MANY_TO_ONE ),
		MANY_TO_MANY( JPADotNames.MANY_TO_MANY ),
		MANY_TO_ANY( HibernateDotNames.MANY_TO_ANY ),
		ELEMENT_COLLECTION_BASIC( JPADotNames.ELEMENT_COLLECTION ),
		ELEMENT_COLLECTION_EMBEDDABLE( JPADotNames.ELEMENT_COLLECTION ),
		EMBEDDED_ID( JPADotNames.EMBEDDED_ID ),
		EMBEDDED( JPADotNames.EMBEDDED );

		private final DotName annotationDotName;

		Nature(DotName annotationDotName) {
			this.annotationDotName = annotationDotName;
		}

		public DotName getAnnotationDotName() {
			return annotationDotName;
		}
	}
}


