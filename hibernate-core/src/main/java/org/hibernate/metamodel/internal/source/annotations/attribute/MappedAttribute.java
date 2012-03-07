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

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.internal.source.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JPADotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.internal.source.annotations.attribute.type.AttributeTypeResolver;
import org.hibernate.metamodel.internal.source.annotations.entity.EntityBindingContext;

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
	AttributeNature attributeNature;

	/**
	 * The access type for this property. At the moment this is either 'field' or 'property', but Hibernate
	 * also allows custom named accessors (see {@link org.hibernate.property.PropertyAccessorFactory}).
	 */
	private final String accessType;

	/**
	 * Defines the column values (relational values) for this property. A mapped property can refer to multiple
	 * column values in case of components or join columns etc
	 */
	private List<Column> columnValues;

	/**
	 * Is this property an id property (or part thereof).
	 */
	private final boolean isId;

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
	 * The binding context
	 */
	private final EntityBindingContext context;

	MappedAttribute(String name, Class<?> attributeType, AttributeNature attributeNature, String accessType, Map<DotName, List<AnnotationInstance>> annotations, EntityBindingContext context) {
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
		checkColumnAnnotations( annotations );
	}

	public String getName() {
		return name;
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

	public AttributeNature getAttributeNature() {
		return attributeNature;
	}

	public String getCheckCondition() {
		return checkCondition;
	}

	@Override
	public int compareTo(MappedAttribute mappedProperty) {
		return name.compareTo( mappedProperty.getName() );
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "MappedAttribute" );
		sb.append( "{name='" ).append( name ).append( '\'' );
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

	private void checkColumnAnnotations(Map<DotName, List<AnnotationInstance>> annotations) {
		columnValues = new ArrayList<Column>();

		// single @Column 
		AnnotationInstance columnAnnotation = JandexHelper.getSingleAnnotation(
				annotations,
				JPADotNames.COLUMN
		);
		if ( columnAnnotation != null ) {
			columnValues.add( new Column( columnAnnotation ) );
		}

		// single @JoinColumn
		AnnotationInstance joinColumnAnnotation = JandexHelper.getSingleAnnotation(
				annotations,
				JPADotNames.JOIN_COLUMN
		);
		if ( joinColumnAnnotation != null ) {
			columnValues.add( new Column( joinColumnAnnotation ) );
		}

		// @org.hibernate.annotations.Columns
		AnnotationInstance columnsAnnotation = JandexHelper.getSingleAnnotation(
				annotations,
				HibernateDotNames.COLUMNS
		);
		if ( columnsAnnotation != null ) {
			List<AnnotationInstance> columnsList = Arrays.asList(
					JandexHelper.getValue( columnsAnnotation, "value", AnnotationInstance[].class )
			);
			for ( AnnotationInstance annotation : columnsList ) {
				columnValues.add( new Column( annotation ) );
			}
		}

		// @JoinColumns
		AnnotationInstance joinColumnsAnnotation = JandexHelper.getSingleAnnotation(
				annotations,
				JPADotNames.JOIN_COLUMNS
		);
		if ( joinColumnsAnnotation != null ) {
			List<AnnotationInstance> columnsList = Arrays.asList(
					JandexHelper.getValue( columnsAnnotation, "value", AnnotationInstance[].class )
			);
			for ( AnnotationInstance annotation : columnsList ) {
				columnValues.add( new Column( annotation ) );
			}
		}
	}

	private String checkCheckAnnotation() {
		String checkCondition = null;
		AnnotationInstance checkAnnotation = JandexHelper.getSingleAnnotation( annotations(), HibernateDotNames.CHECK );
		if ( checkAnnotation != null ) {
			checkCondition = checkAnnotation.value( "constraints" ).toString();
		}
		return checkCondition;
	}
}


