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
package org.hibernate.metamodel.source.annotations.entity;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.AccessType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.metamodel.binding.InheritanceType;
import org.hibernate.metamodel.source.annotations.AnnotationBindingContext;
import org.hibernate.metamodel.source.annotations.JPADotNames;
import org.hibernate.metamodel.source.annotations.util.JandexHelper;

/**
 * Represents an entity or mapped superclass configured via annotations/xml.
 *
 * @author Hardy Ferentschik
 */
public class EntityClass extends ConfiguredClass {
	private final AccessType hierarchyAccessType;
	private final InheritanceType inheritanceType;
	private final boolean hasOwnTable;
	private final String primaryTableName;
	private final IdType idType;
	private final EntityClass jpaEntityParent;

	public EntityClass(ClassInfo classInfo,
					   EntityClass parent,
					   AccessType hierarchyAccessType,
					   InheritanceType inheritanceType,
					   AnnotationBindingContext context) {

		super( classInfo, hierarchyAccessType, parent, context );
		this.hierarchyAccessType = hierarchyAccessType;
		this.inheritanceType = inheritanceType;
		this.idType = determineIdType();
		this.jpaEntityParent = findJpaEntitySuperClass();
		this.hasOwnTable = definesItsOwnTable();
		this.primaryTableName = determinePrimaryTableName();
	}

	/**
	 * @return Returns the next JPA super entity for this entity class or {@code null} in case there is none.
	 */
	public EntityClass getEntityParent() {
		return jpaEntityParent;
	}

	/**
	 * @return Returns {@code true} is this entity class is the root of the class hierarchy in the JPA sense, which
	 *         means there are no more super classes which are annotated with @Entity. There can, however, be mapped superclasses
	 *         or non entities in the actual java type hierarchy.
	 */
	public boolean isEntityRoot() {
		return jpaEntityParent == null;
	}

	public InheritanceType getInheritanceType() {
		return inheritanceType;
	}

	public IdType getIdType() {
		return idType;
	}

	public boolean hasOwnTable() {
		return hasOwnTable;
	}

	public String getPrimaryTableName() {
		return primaryTableName;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "EntityClass" );
		sb.append( "{name=" ).append( getConfiguredClass().getSimpleName() );
		sb.append( ", hierarchyAccessType=" ).append( hierarchyAccessType );
		sb.append( ", inheritanceType=" ).append( inheritanceType );
		sb.append( ", hasOwnTable=" ).append( hasOwnTable );
		sb.append( ", primaryTableName='" ).append( primaryTableName ).append( '\'' );
		sb.append( ", idType=" ).append( idType );
		sb.append( '}' );
		return sb.toString();
	}

	private boolean definesItsOwnTable() {
		// mapped super classes don't have their own tables
		if ( ConfiguredClassType.MAPPED_SUPERCLASS.equals( getConfiguredClassType() ) ) {
			return false;
		}

		if ( InheritanceType.SINGLE_TABLE.equals( inheritanceType ) ) {
			if ( isEntityRoot() ) {
				return true;
			}
			else {
				return false;
			}
		}
		return true;
	}

	private EntityClass findJpaEntitySuperClass() {
		ConfiguredClass tmpConfiguredClass = this.getParent();
		while ( tmpConfiguredClass != null ) {
			if ( ConfiguredClassType.ENTITY.equals( tmpConfiguredClass.getConfiguredClassType() ) ) {
				return (EntityClass) tmpConfiguredClass;
			}
			tmpConfiguredClass = tmpConfiguredClass.getParent();
		}
		return null;
	}

	private String determinePrimaryTableName() {
		String tableName = null;
		if ( hasOwnTable() ) {
			tableName = getConfiguredClass().getSimpleName();
			AnnotationInstance tableAnnotation = JandexHelper.getSingleAnnotation(
					getClassInfo(), JPADotNames.TABLE
			);
			if ( tableAnnotation != null ) {
				AnnotationValue value = tableAnnotation.value( "name" );
				String tmp = value == null ? null : value.asString();
				if ( tmp != null && !tmp.isEmpty() ) {
					tableName = tmp;
				}
			}
		}
		else if ( getParent() != null
				&& !getParent().getConfiguredClassType().equals( ConfiguredClassType.MAPPED_SUPERCLASS ) ) {
			tableName = ( (EntityClass) getParent() ).getPrimaryTableName();
		}
		return tableName;
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
			if ( idAnnotations.size() == 1 ) {
				return IdType.SIMPLE;
			}
			else {
				return IdType.COMPOSED;
			}
		}
		return IdType.NONE;
	}

	private List<AnnotationInstance> findIdAnnotations(DotName idAnnotationType) {
		List<AnnotationInstance> idAnnotationList = new ArrayList<AnnotationInstance>();
		if ( getClassInfo().annotations().get( idAnnotationType ) != null ) {
			idAnnotationList.addAll( getClassInfo().annotations().get( idAnnotationType ) );
		}
		ConfiguredClass parent = getParent();
		while ( parent != null && ( ConfiguredClassType.MAPPED_SUPERCLASS.equals( parent.getConfiguredClassType() ) ||
				ConfiguredClassType.NON_ENTITY.equals( parent.getConfiguredClassType() ) ) ) {
			if ( parent.getClassInfo().annotations().get( idAnnotationType ) != null ) {
				idAnnotationList.addAll( parent.getClassInfo().annotations().get( idAnnotationType ) );
			}
			parent = parent.getParent();

		}
		return idAnnotationList;
	}
}
