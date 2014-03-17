/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.metamodel.internal.binder;

import java.util.Map;

import javax.persistence.ConstraintMode;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JandexHelper;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

/**
 * @author Brett Meyer
 */
public class ForeignKeyDelegate {

	private String explicitForeignKeyName = null;

	private String inverseForeignKeyName = null;

	private boolean createForeignKeyConstraint = true;

	// TODO: do we need inverseCreateForeignKeyConstraint?

	public ForeignKeyDelegate() {
	}

	public ForeignKeyDelegate(Map<DotName, AnnotationInstance> annotations, ClassLoaderService cls) {
		final AnnotationInstance joinColumnAnnotation = annotations.get( JPADotNames.JOIN_COLUMN );
		final AnnotationInstance joinTable = annotations.get( JPADotNames.JOIN_TABLE );
		final AnnotationInstance collectionTable = annotations.get( JPADotNames.COLLECTION_TABLE );

		if ( joinColumnAnnotation != null ) {
			resolveExplicitName( joinColumnAnnotation, cls );
			resolveCreateForeignKeyConstraint( joinColumnAnnotation, cls );
		}
		if ( joinTable != null ) {
			resolveExplicitName( joinTable, cls );
			resolveInverseName( joinTable, cls );
			resolveCreateForeignKeyConstraint( joinTable, cls );
		}
		if ( collectionTable != null ) {
			resolveExplicitName( collectionTable, cls );
			resolveCreateForeignKeyConstraint( collectionTable, cls );
		}
	}

	private void resolveExplicitName(AnnotationInstance owningAnnotation, ClassLoaderService cls) {
		AnnotationInstance jpaFkAnnotation = JandexHelper.getValue( owningAnnotation, "foreignKey", AnnotationInstance.class, cls );
		explicitForeignKeyName = jpaFkAnnotation != null ? JandexHelper.getValue( jpaFkAnnotation, "name", String.class, cls ) : null;
	}

	private void resolveInverseName(AnnotationInstance owningAnnotation, ClassLoaderService cls) {
		AnnotationInstance jpaFkAnnotation = JandexHelper.getValue( owningAnnotation, "inverseForeignKey", AnnotationInstance.class, cls );
		inverseForeignKeyName = jpaFkAnnotation != null ? JandexHelper.getValue( jpaFkAnnotation, "name", String.class, cls ) : null;
	}

	private void resolveCreateForeignKeyConstraint(AnnotationInstance owningAnnotation, ClassLoaderService cls) {
		AnnotationInstance jpaFkAnnotation = JandexHelper.getValue( owningAnnotation, "foreignKey", AnnotationInstance.class, cls );
		if ( jpaFkAnnotation != null ) {
			ConstraintMode mode = JandexHelper.getEnumValue( jpaFkAnnotation, "value", ConstraintMode.class, cls );
			createForeignKeyConstraint = !mode.equals( ConstraintMode.NO_CONSTRAINT );
		}
	}

	public String getExplicitForeignKeyName() {
		return explicitForeignKeyName;
	}

	public String getInverseForeignKeyName() {
		return inverseForeignKeyName;
	}

	public boolean createForeignKeyConstraint() {
		return createForeignKeyConstraint;
	}
}
