/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal.source.annotations.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.persistence.AccessType;
import javax.persistence.DiscriminatorType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.metamodel.internal.source.annotations.AnnotationBindingContext;
import org.hibernate.metamodel.internal.source.annotations.attribute.BasicAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.Column;
import org.hibernate.metamodel.internal.source.annotations.attribute.FormulaValue;
import org.hibernate.metamodel.internal.source.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JPADotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.binding.InheritanceType;

/**
 * Represents an root entity configured via annotations/orm-xml.
 *
 * @author Hardy Ferentschik
 */
public class RootEntityClass extends EntityClass {

	private final IdType idType;
	private final List<MappedSuperclass> mappedSuperclasses;

	// discriminator related fields
	private Column discriminatorColumnValues;
	private FormulaValue discriminatorFormula;
	private Class<?> discriminatorType;

	private boolean isDiscriminatorForced = false;
	private boolean isDiscriminatorIncludedInSql = true;
	private boolean needsDiscriminatorColumn = false;


	/**
	 * Constructor used for entity roots
	 *
	 * @param classInfo the jandex class info this this entity
	 * @param mappedSuperclasses a list of class info instances representing the mapped super classes for this root entity
	 * @param hierarchyAccessType the default access type
	 * @param inheritanceType the inheritance type this entity
	 * @param hasSubclasses flag indicating whether this root entity has sub classes
	 * @param context the binding context
	 */
	public RootEntityClass(
			ClassInfo classInfo,
			List<ClassInfo> mappedSuperclasses,
			AccessType hierarchyAccessType,
			InheritanceType inheritanceType,
			boolean hasSubclasses,
			AnnotationBindingContext context) {
		super( classInfo, null, hierarchyAccessType, inheritanceType, context );
		this.mappedSuperclasses = new ArrayList<MappedSuperclass>();
		for ( ClassInfo mappedSuperclassInfo : mappedSuperclasses ) {
			MappedSuperclass configuredClass = new MappedSuperclass(
					mappedSuperclassInfo,
					null,
					hierarchyAccessType,
					context
			);
			this.mappedSuperclasses.add( configuredClass );
		}

		if ( InheritanceType.SINGLE_TABLE.equals( inheritanceType ) ) {
			processDiscriminator();
			if ( hasSubclasses || isDiscriminatorForced ) {
				needsDiscriminatorColumn = true;
			}
		}

		this.idType = determineIdType();
	}

	public boolean needsDiscriminatorColumn() {
		return needsDiscriminatorColumn;
	}

	public Column getDiscriminatorColumnValues() {
		return discriminatorColumnValues;
	}

	public FormulaValue getDiscriminatorFormula() {
		return discriminatorFormula;
	}

	public Class<?> getDiscriminatorType() {
		return discriminatorType;
	}

	public IdType getIdType() {
		return idType;
	}

	public boolean isDiscriminatorForced() {
		return isDiscriminatorForced;
	}

	public boolean isDiscriminatorIncludedInSql() {
		return isDiscriminatorIncludedInSql;
	}

	@Override
	public Collection<BasicAttribute> getSimpleAttributes() {
		List<BasicAttribute> attributes = new ArrayList<BasicAttribute>();

		// add the attributes defined on this entity directly
		attributes.addAll( super.getSimpleAttributes() );

		// now the attributes of the mapped superclasses
		// TODO - take care of overrides (HF)
		for ( MappedSuperclass mappedSuperclass : mappedSuperclasses ) {
			attributes.addAll( mappedSuperclass.getSimpleAttributes() );
		}

		return attributes;
	}

	private IdType determineIdType() {
		List<AnnotationInstance> idAnnotations = findIdAnnotations( JPADotNames.ID );
		List<AnnotationInstance> embeddedIdAnnotations = findIdAnnotations( JPADotNames.EMBEDDED_ID );

		if ( !idAnnotations.isEmpty() && !embeddedIdAnnotations.isEmpty() ) {
			throw new MappingException(
					"@EmbeddedId and @Id cannot be used together. Check the configuration for " + getName() + "."
			);
		}

		if ( !embeddedIdAnnotations.isEmpty() ) {
			if ( embeddedIdAnnotations.size() == 1 ) {
				return IdType.EMBEDDED;
			}
			else {
				throw new AnnotationException( "Multiple @EmbeddedId annotations are not allowed" );
			}
		}

		if ( !idAnnotations.isEmpty() ) {
			return idAnnotations.size() == 1 ? IdType.SIMPLE : IdType.COMPOSED;
		}
		return IdType.NONE;
	}

	private List<AnnotationInstance> findIdAnnotations(DotName idAnnotationType) {
		List<AnnotationInstance> idAnnotationList = new ArrayList<AnnotationInstance>();
		if ( getClassInfo().annotations().containsKey( idAnnotationType ) ) {
			idAnnotationList.addAll( getClassInfo().annotations().get( idAnnotationType ) );
		}
		ConfiguredClass parent = getParent();
		while ( parent != null ) {
			if ( parent.getClassInfo().annotations().containsKey( idAnnotationType ) ) {
				idAnnotationList.addAll( parent.getClassInfo().annotations().get( idAnnotationType ) );
			}
			parent = parent.getParent();
		}
		return idAnnotationList;
	}

	private void processDiscriminator() {
		final AnnotationInstance discriminatorColumnAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(), JPADotNames.DISCRIMINATOR_COLUMN
		);

		final AnnotationInstance discriminatorFormulaAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(),
				HibernateDotNames.DISCRIMINATOR_FORMULA
		);


		Class<?> type = String.class; // string is the discriminator default
		if ( discriminatorFormulaAnnotation != null ) {
			String expression = JandexHelper.getValue( discriminatorFormulaAnnotation, "value", String.class );
			discriminatorFormula = new FormulaValue( null, expression );
		}
		discriminatorColumnValues = new Column( null ); //(stliu) give null here, will populate values below
		discriminatorColumnValues.setNullable( false ); // discriminator column cannot be null
		if ( discriminatorColumnAnnotation != null ) {

			DiscriminatorType discriminatorType = Enum.valueOf(
					DiscriminatorType.class, discriminatorColumnAnnotation.value( "discriminatorType" ).asEnum()
			);
			switch ( discriminatorType ) {
				case STRING: {
					type = String.class;
					break;
				}
				case CHAR: {
					type = Character.class;
					break;
				}
				case INTEGER: {
					type = Integer.class;
					break;
				}
				default: {
					throw new AnnotationException( "Unsupported discriminator type: " + discriminatorType );
				}
			}

			discriminatorColumnValues.setName(
					JandexHelper.getValue(
							discriminatorColumnAnnotation,
							"name",
							String.class
					)
			);
			discriminatorColumnValues.setLength(
					JandexHelper.getValue(
							discriminatorColumnAnnotation,
							"length",
							Integer.class
					)
			);
			discriminatorColumnValues.setColumnDefinition(
					JandexHelper.getValue(
							discriminatorColumnAnnotation,
							"columnDefinition",
							String.class
					)
			);
		}
		discriminatorType = type;

		AnnotationInstance discriminatorOptionsAnnotation = JandexHelper.getSingleAnnotation(
				getClassInfo(), HibernateDotNames.DISCRIMINATOR_OPTIONS
		);
		if ( discriminatorOptionsAnnotation != null ) {
			isDiscriminatorForced = discriminatorOptionsAnnotation.value( "force" ).asBoolean();
			isDiscriminatorIncludedInSql = discriminatorOptionsAnnotation.value( "insert" ).asBoolean();
		}
		else {
			isDiscriminatorForced = false;
			isDiscriminatorIncludedInSql = true;
		}
	}
}
