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

import java.util.List;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

import org.hibernate.metamodel.internal.source.annotations.HibernateDotNames;
import org.hibernate.metamodel.internal.source.annotations.JandexHelper;
import org.hibernate.metamodel.internal.source.annotations.entity.EntityBindingContext;

/**
 * Represents an association attribute.
 *
 * @author Hardy Ferentschik
 */
public class PluralAssociationAttribute extends AssociationAttribute {
	private final String whereClause;
	private final String orderBy;

	public static PluralAssociationAttribute createPluralAssociationAttribute(String name,
																			  Class<?> attributeType,
																			  AttributeNature attributeNature,
																			  String accessType,
																			  Map<DotName, List<AnnotationInstance>> annotations,
																			  EntityBindingContext context) {
		return new PluralAssociationAttribute(
				name,
				attributeType,
				attributeNature,
				accessType,
				annotations,
				context
		);
	}

	private PluralAssociationAttribute(String name,
									   Class<?> javaType,
									   AttributeNature associationType,
									   String accessType,
									   Map<DotName, List<AnnotationInstance>> annotations,
									   EntityBindingContext context) {
		super( name, javaType, associationType, accessType, annotations, context );
		this.whereClause = determineWereClause();
		this.orderBy = determineOrderBy();
	}

	private String determineWereClause() {
		String where = null;

		AnnotationInstance whereAnnotation = JandexHelper.getSingleAnnotation( annotations(), HibernateDotNames.WHERE );
		if ( whereAnnotation != null ) {
			where = JandexHelper.getValue( whereAnnotation, "clause", String.class );
		}

		return where;
	}

	private String determineOrderBy() {
		String orderBy = null;

		AnnotationInstance whereAnnotation = JandexHelper.getSingleAnnotation(
				annotations(),
				HibernateDotNames.ORDER_BY
		);
		if ( whereAnnotation != null ) {
			orderBy = JandexHelper.getValue( whereAnnotation, "clause", String.class );
		}

		return orderBy;
	}

	public String getWhereClause() {
		return whereClause;
	}

	public String getOrderBy() {
		return orderBy;
	}
}


