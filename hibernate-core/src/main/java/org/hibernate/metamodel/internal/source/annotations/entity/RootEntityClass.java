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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.AccessType;
import javax.persistence.DiscriminatorType;

import org.hibernate.AnnotationException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.internal.source.annotations.AnnotationBindingContext;
import org.hibernate.metamodel.internal.source.annotations.attribute.AssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.AssociationOverride;
import org.hibernate.metamodel.internal.source.annotations.attribute.AttributeOverride;
import org.hibernate.metamodel.internal.source.annotations.attribute.BasicAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.Column;
import org.hibernate.metamodel.internal.source.annotations.attribute.FormulaValue;
import org.hibernate.metamodel.internal.source.annotations.attribute.MappedAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.PrimaryKeyJoinColumn;
import org.hibernate.metamodel.internal.source.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JPADotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.binding.InheritanceType;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import com.fasterxml.classmate.ResolvedTypeWithMembers;

/**
 * Represents an root entity configured via annotations/orm-xml.
 *
 * @author Hardy Ferentschik
 * @author Brett Meyer
 */
public class RootEntityClass extends EntityClass {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			RootEntityClass.class.getName()
	);

	private final IdType idType;
	private final List<MappedSuperclass> mappedSuperclasses= new ArrayList<MappedSuperclass>();

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
			Map<String, ResolvedTypeWithMembers> resolvedTypeWithMembers,
			List<ClassInfo> mappedSuperclasses,
			AccessType hierarchyAccessType,
			InheritanceType inheritanceType,
			boolean hasSubclasses,
			AnnotationBindingContext context) {
		super( classInfo, resolvedTypeWithMembers.get( classInfo.toString() ), null, hierarchyAccessType, inheritanceType, context );
		for ( ClassInfo mappedSuperclassInfo : mappedSuperclasses ) {
			MappedSuperclass configuredClass = new MappedSuperclass(
					mappedSuperclassInfo,
					resolvedTypeWithMembers.get( mappedSuperclassInfo.toString() ),
					null,
					hierarchyAccessType,
					context
			);
			this.mappedSuperclasses.add( configuredClass );
		}
		if ( InheritanceType.SINGLE_TABLE.equals( inheritanceType ) ) {
			processDiscriminator();
			this.needsDiscriminatorColumn = hasSubclasses || isDiscriminatorForced;
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

	public List<MappedSuperclass> getMappedSuperclasses() {
		return mappedSuperclasses;
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

	protected List<PrimaryKeyJoinColumn> determinePrimaryKeyJoinColumns() {
		List<PrimaryKeyJoinColumn> results = super.determinePrimaryKeyJoinColumns();
		if ( CollectionHelper.isNotEmpty( results ) ) {
			LOG.invalidPrimaryKeyJoinColumnAnnotation();
		}
		return null;
	}

	private final ValueHolder<Map<String, BasicAttribute>> simpleAttributes = new ValueHolder<Map<String, BasicAttribute>>(
			new ValueHolder.DeferredInitializer<Map<String, BasicAttribute>>() {
				@Override
				public Map<String, BasicAttribute> initialize() {
					final Map<String, BasicAttribute> map = new HashMap<String, BasicAttribute>();
					for ( MappedSuperclass mappedSuperclass : getMappedSuperclasses() ) {
						map.putAll( mappedSuperclass.getSimpleAttributes() );
					}
					map.putAll( RootEntityClass.super.getSimpleAttributes() );
					return map;
				}
			}
	);

	@Override
	public Map<String,BasicAttribute> getSimpleAttributes() {
		return simpleAttributes.getValue();
	}

	private final ValueHolder<Map<String, AssociationAttribute>> associationAttributes = new ValueHolder<Map<String, AssociationAttribute>>(
			new ValueHolder.DeferredInitializer<Map<String, AssociationAttribute>>() {
				@Override
				public Map<String, AssociationAttribute> initialize() {
					Map<String, AssociationAttribute> map = new HashMap<String, AssociationAttribute>();
					for ( MappedSuperclass mappedSuperclass : getMappedSuperclasses() ) {
						map.putAll( mappedSuperclass.getAssociationAttributes() );
					}
					map.putAll( RootEntityClass.super.getAssociationAttributes() );
					return map;
				}
			}
	);

	@Override
	public Map<String, AssociationAttribute> getAssociationAttributes() {
		return associationAttributes.getValue();
	}

	private final ValueHolder<Map<String, AttributeOverride>> attributeOverrideMap = new ValueHolder<Map<String, AttributeOverride>>(
			new ValueHolder.DeferredInitializer<Map<String, AttributeOverride>>() {
				@Override
				public Map<String, AttributeOverride> initialize() {
					if ( CollectionHelper.isEmpty( getMappedSuperclasses() ) ) {
						return RootEntityClass.super.getAttributeOverrideMap();
					}
					Map<String, AttributeOverride> map = new HashMap<String, AttributeOverride>();
					for ( MappedSuperclass mappedSuperclass : getMappedSuperclasses() ) {
						map.putAll( mappedSuperclass.getAttributeOverrideMap() );
					}
					map.putAll( RootEntityClass.super.getAttributeOverrideMap() );
					return map;
				}
			}
	);

	private final ValueHolder<Map<String, AssociationOverride>> associationOverrideMap = new ValueHolder<Map<String, AssociationOverride>>(
			new ValueHolder.DeferredInitializer<Map<String, AssociationOverride>>() {
				@Override
				public Map<String, AssociationOverride> initialize() {
					if ( CollectionHelper.isEmpty( getMappedSuperclasses() ) ) {
						return RootEntityClass.super.getAssociationOverrideMap();
					}
					Map<String, AssociationOverride> map = new HashMap<String, AssociationOverride>();
					for ( MappedSuperclass mappedSuperclass : getMappedSuperclasses() ) {
						map.putAll( mappedSuperclass.getAssociationOverrideMap() );
					}
					map.putAll( RootEntityClass.super.getAssociationOverrideMap() );
					return map;
				}
			}
	);


	@Override
	public Map<String, AssociationOverride> getAssociationOverrideMap() {
		return associationOverrideMap.getValue();
	}

	@Override
	public Map<String, AttributeOverride> getAttributeOverrideMap() {
		return attributeOverrideMap.getValue();
	}

	private final ValueHolder<Map<String,MappedAttribute>> idAttributes = new ValueHolder<Map<String,MappedAttribute>>(
			new ValueHolder.DeferredInitializer<Map<String,MappedAttribute>>() {
				@Override
				public Map<String,MappedAttribute> initialize() {
					Map<String,MappedAttribute> attributes = new HashMap<String, MappedAttribute>();

					// get all id attributes defined on this entity
					attributes.putAll( RootEntityClass.super.getIdAttributes() );

					// now mapped super classes
					for ( MappedSuperclass mappedSuperclass : mappedSuperclasses ) {
						attributes.putAll( mappedSuperclass.getIdAttributes() );
					}

					return attributes;
				}
			}
	);

	public Map<String,MappedAttribute> getIdAttributes() {
		return idAttributes.getValue();
	}
	
	public AnnotationInstance getIdClassAnnotation() {
		// TODO: refactor
		final List<AnnotationInstance> idClassAnnotations = findIdAnnotations(
				JPADotNames.ID_CLASS
		);
		
		return ( idClassAnnotations.size() > 0 ) ? idClassAnnotations.get( 0 ) : null;
	}

	private IdType determineIdType() {
		Collection<MappedAttribute> idAttributes = getIdAttributes().values();
		int size = idAttributes.size();
		switch ( size ){
			case 0:
				return IdType.NONE;
			case 1:
				MappedAttribute idAttribute = idAttributes.iterator().next();
				switch ( idAttribute.getNature() ){
					case BASIC:
						return IdType.SIMPLE;
					case EMBEDDED_ID:
						return IdType.EMBEDDED;
				}
			default:
				return IdType.COMPOSED;
		}
	}

	private List<AnnotationInstance> findIdAnnotations(DotName idAnnotationType) {
		List<AnnotationInstance> idAnnotationList = new ArrayList<AnnotationInstance>();

		// check the class itself
		if ( getClassInfo().annotations().containsKey( idAnnotationType ) ) {
			idAnnotationList.addAll( getClassInfo().annotations().get( idAnnotationType ) );
		}

		// check mapped super classes
		for ( MappedSuperclass mappedSuperclass : mappedSuperclasses ) {
			if ( mappedSuperclass.getClassInfo().annotations().containsKey( idAnnotationType ) ) {
				idAnnotationList.addAll( mappedSuperclass.getClassInfo().annotations().get( idAnnotationType ) );
			}
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
			String expression = JandexHelper.getValue( discriminatorFormulaAnnotation, "value", String.class,
					getLocalBindingContext().getServiceRegistry().getService( ClassLoaderService.class ) );
			discriminatorFormula = new FormulaValue( null, expression );
		}
		discriminatorColumnValues = new Column( null ); //(stliu) give null here, will populate values below
		discriminatorColumnValues.setNullable( false ); // discriminator column cannot be null
		if ( discriminatorColumnAnnotation != null ) {
			DiscriminatorType discriminatorType = JandexHelper.getEnumValue( 
					discriminatorColumnAnnotation,
					"discriminatorType", DiscriminatorType.class,
					getLocalBindingContext().getServiceRegistry().getService( ClassLoaderService.class ) );
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
							String.class,
							getLocalBindingContext().getServiceRegistry().getService( ClassLoaderService.class )
					)
			);
			discriminatorColumnValues.setLength(
					JandexHelper.getValue(
							discriminatorColumnAnnotation,
							"length",
							Integer.class,
							getLocalBindingContext().getServiceRegistry().getService( ClassLoaderService.class )
					)
			);
			discriminatorColumnValues.setColumnDefinition(
					JandexHelper.getValue(
							discriminatorColumnAnnotation,
							"columnDefinition",
							String.class,
							getLocalBindingContext().getServiceRegistry().getService( ClassLoaderService.class )
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
