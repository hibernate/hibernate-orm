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
package org.hibernate.metamodel.source.annotations;

import java.util.List;
import javax.persistence.AccessType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;

import org.hibernate.AnnotationException;

/**
 * Represents an entity, mapped superclass or component configured via annotations/xml.
 *
 * @author Hardy Ferentschik
 */
public class ConfiguredClass {
	private final ClassInfo classInfo;
	private final AccessType classAccessType;
	private final ConfiguredClassHierarchy hierarchy;
	private final boolean isMappedSuperClass;
	private final List<MappedProperty> mappedProperties;

	public ConfiguredClass(ClassInfo info, ConfiguredClassHierarchy hierarchy) {
		this.classInfo = info;
		this.hierarchy = hierarchy;

		//@Entity and @MappedSuperclass on the same class leads to a NPE down the road
		AnnotationInstance jpaEntityAnnotation = JandexHelper.getSingleAnnotation( classInfo, JPADotNames.ENTITY );
		AnnotationInstance mappedSuperClassAnnotation = JandexHelper.getSingleAnnotation(
				classInfo, JPADotNames.MAPPED_SUPER_CLASS
		);

		if ( jpaEntityAnnotation != null && mappedSuperClassAnnotation != null ) {
			throw new AnnotationException(
					"An entity cannot be annotated with both @Entity and @MappedSuperclass: "
							+ classInfo.name().toString()
			);
		}

		isMappedSuperClass = mappedSuperClassAnnotation != null;
		classAccessType = determineClassAccessType( hierarchy.getDefaultAccessType() );
		mappedProperties = collectMappedProperties();
	}

	public ClassInfo getClassInfo() {
		return classInfo;
	}

	public boolean isMappedSuperClass() {
		return isMappedSuperClass;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "ConfiguredClass" );
		sb.append( "{classInfo=" ).append( classInfo );
		sb.append( '}' );
		return sb.toString();
	}

	private AccessType determineClassAccessType(AccessType hierarchyAccessType) {
		// default to the hierarchy access type to start with
		AccessType accessType = hierarchyAccessType;

		AnnotationInstance accessAnnotation = JandexHelper.getSingleAnnotation( classInfo, JPADotNames.ACCESS );
		if ( accessAnnotation != null ) {
			accessType = Enum.valueOf( AccessType.class, accessAnnotation.value( "value" ).asEnum() );
		}

		return accessType;
	}

	private List<MappedProperty> collectMappedProperties() {
		return null;  //To change body of created methods use File | Settings | File Templates.
	}
}


