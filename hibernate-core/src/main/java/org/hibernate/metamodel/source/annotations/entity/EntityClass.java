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

import java.util.List;
import javax.persistence.AccessType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.metamodel.binding.InheritanceType;
import org.hibernate.metamodel.source.annotations.AnnotationBindingContext;
import org.hibernate.metamodel.source.annotations.JPADotNames;
import org.hibernate.metamodel.source.annotations.util.JandexHelper;

/**
 * Represents an entity, mapped superclass or component configured via annotations/xml.
 *
 * @author Hardy Ferentschik
 */
public class EntityClass extends ConfiguredClass {
	private final AccessType hierarchyAccessType;

	private final InheritanceType inheritanceType;
	private final boolean hasOwnTable;
	private final String primaryTableName;

	private final IdType idType;

	public EntityClass(ClassInfo classInfo,
					   EntityClass parent,
					   AccessType hierarchyAccessType,
					   InheritanceType inheritanceType,
					   AnnotationBindingContext context) {

		super( classInfo, hierarchyAccessType, parent, context );
		this.hierarchyAccessType = hierarchyAccessType;
		this.inheritanceType = inheritanceType;
		this.idType = determineIdType();

		this.hasOwnTable = definesItsOwnTable();
		this.primaryTableName = determinePrimaryTableName();
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
		sb.append( "{name=" ).append( getName() );
		sb.append( ", hierarchyAccessType=" ).append( hierarchyAccessType );
		sb.append( ", inheritanceType=" ).append( inheritanceType );
		sb.append( ", hasOwnTable=" ).append( hasOwnTable );
		sb.append( ", primaryTableName='" ).append( primaryTableName ).append( '\'' );
		sb.append( ", idType=" ).append( idType );
		sb.append( '}' );
		return sb.toString();
	}

	private boolean definesItsOwnTable() {
		// mapped super classes and embeddables don't have their own tables
		if ( ConfiguredClassType.MAPPED_SUPERCLASS.equals( getConfiguredClassType() ) || ConfiguredClassType.EMBEDDABLE
				.equals( getConfiguredClassType() ) ) {
			return false;
		}

		if ( InheritanceType.SINGLE_TABLE.equals( inheritanceType ) ) {
			return isRoot();
		}
		return true;
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
				&& !getParent().getConfiguredClassType().equals( ConfiguredClassType.MAPPED_SUPERCLASS )
				&& !getParent().getConfiguredClassType().equals( ConfiguredClassType.EMBEDDABLE ) ) {
			tableName = ( (EntityClass) getParent() ).getPrimaryTableName();
		}
		return tableName;
	}

	private IdType determineIdType() {
		List<AnnotationInstance> idAnnotations = getClassInfo().annotations().get( JPADotNames.ID );
		List<AnnotationInstance> embeddedIdAnnotations = getClassInfo()
				.annotations()
				.get( JPADotNames.EMBEDDED_ID );

		if ( idAnnotations != null && embeddedIdAnnotations != null ) {
			throw new MappingException(
					"@EmbeddedId and @Id cannot be used together. Check the configuration for " + getName() + "."
			);
		}

		if ( embeddedIdAnnotations != null ) {
			if ( embeddedIdAnnotations.size() == 1 ) {
				return IdType.EMBEDDED;
			}
			else {
				throw new AnnotationException( "Multiple @EmbeddedId annotations are not allowed" );
			}
		}

		if ( idAnnotations != null ) {
			if ( idAnnotations.size() == 1 ) {
				return IdType.SIMPLE;
			}
			else {
				return IdType.COMPOSED;
			}
		}
		return IdType.NONE;
	}
}
