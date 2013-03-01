/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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

import java.util.List;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import org.hibernate.metamodel.internal.source.annotations.entity.EntityBindingContext;
import org.hibernate.metamodel.internal.source.annotations.util.JPADotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;

/**
 * @author Gail Badner
 */
public class SingularAssociationAttribute extends AssociationAttribute {
	private final boolean hasPrimaryKeyJoinColumn;
	public static AssociationAttribute createSingularAssociationAttribute(
			ClassInfo classInfo,
			String name,
			Class<?> attributeType,
			Nature attributeNature,
			String accessType,
			Map<DotName, List<AnnotationInstance>> annotations,
			EntityBindingContext context) {
		return new SingularAssociationAttribute(
				classInfo,
				name,
				attributeType,
				attributeType,
				attributeNature,
				accessType,
				annotations,
				context
		);
	}

	SingularAssociationAttribute(
			ClassInfo classInfo,
			String name,
			Class<?> attributeType,
			Class<?> referencedAttributeType,
			Nature attributeNature,
			String accessType,
			Map<DotName, List<AnnotationInstance>> annotations,
			EntityBindingContext context) {
		super( classInfo, name, attributeType, referencedAttributeType, attributeNature, accessType, annotations, context );
		this.hasPrimaryKeyJoinColumn =
				JandexHelper.containsSingleAnnotation( annotations, JPADotNames.PRIMARY_KEY_JOIN_COLUMN ) ||
						JandexHelper.containsSingleAnnotation( annotations, JPADotNames.PRIMARY_KEY_JOIN_COLUMNS );
	}

	public boolean hasPrimaryKeyJoinColumn() {
		return hasPrimaryKeyJoinColumn;
	}


}