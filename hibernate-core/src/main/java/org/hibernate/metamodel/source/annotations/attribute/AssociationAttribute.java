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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.CascadeType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;

import org.hibernate.annotations.NotFoundAction;
import org.hibernate.metamodel.binder.source.annotations.HibernateDotNames;
import org.hibernate.metamodel.binder.source.annotations.JandexHelper;

/**
 * @author Hardy Ferentschik
 */
public class AssociationAttribute extends SimpleAttribute {
	private final AttributeType associationType;
	private final boolean ignoreNotFound;
	private final String referencedEntityType;
	private final Set<CascadeType> cascadeTypes;

	public static AssociationAttribute createAssociationAttribute(String name, String type, AttributeType associationType, Map<DotName, List<AnnotationInstance>> annotations) {
		return new AssociationAttribute( name, type, associationType, annotations );
	}

	private AssociationAttribute(String name, String type, AttributeType associationType, Map<DotName, List<AnnotationInstance>> annotations) {
		super( name, type, annotations, false );
		this.associationType = associationType;
		this.ignoreNotFound = ignoreNotFound();

		AnnotationInstance associationAnnotation = JandexHelper.getSingleAnnotation(
				annotations,
				associationType.getAnnotationDotName()
		);

		referencedEntityType = determineReferencedEntityType( associationAnnotation );
		cascadeTypes = determineCascadeTypes( associationAnnotation );
	}

	public boolean isIgnoreNotFound() {
		return ignoreNotFound;
	}

	public String getReferencedEntityType() {
		return referencedEntityType;
	}

	public AttributeType getAssociationType() {
		return associationType;
	}

	public Set<CascadeType> getCascadeTypes() {
		return cascadeTypes;
	}

	private boolean ignoreNotFound() {
		NotFoundAction action = NotFoundAction.EXCEPTION;
		AnnotationInstance notFoundAnnotation = getIfExists( HibernateDotNames.NOT_FOUND );
		if ( notFoundAnnotation != null ) {
			AnnotationValue actionValue = notFoundAnnotation.value( "action" );
			if ( actionValue != null ) {
				action = Enum.valueOf( NotFoundAction.class, actionValue.asEnum() );
			}
		}

		return NotFoundAction.IGNORE.equals( action );
	}

	private String determineReferencedEntityType(AnnotationInstance associationAnnotation) {
		String targetTypeName = getType();

		AnnotationInstance targetAnnotation = getIfExists( HibernateDotNames.TARGET );
		if ( targetAnnotation != null ) {
			targetTypeName = targetAnnotation.value().asClass().name().toString();
		}

		AnnotationValue targetEntityValue = associationAnnotation.value( "targetEntity" );
		if ( targetEntityValue != null ) {
			targetTypeName = targetEntityValue.asClass().name().toString();
		}

		return targetTypeName;
	}

	private Set<CascadeType> determineCascadeTypes(AnnotationInstance associationAnnotation) {
		Set<CascadeType> cascadeTypes = new HashSet<CascadeType>();
		AnnotationValue cascadeValue = associationAnnotation.value( "cascade" );
		if ( cascadeValue != null ) {
			String[] cascades = cascadeValue.asEnumArray();
			for ( String s : cascades ) {
				cascadeTypes.add( Enum.valueOf( CascadeType.class, s ) );
			}
		}
		return cascadeTypes;
	}
}


