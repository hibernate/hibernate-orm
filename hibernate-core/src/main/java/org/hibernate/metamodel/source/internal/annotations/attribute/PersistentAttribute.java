/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.internal.annotations.attribute;

import javax.persistence.AccessType;

import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.reflite.spi.MemberDescriptor;
import org.hibernate.metamodel.source.internal.annotations.attribute.type.AttributeTypeResolver;
import org.hibernate.metamodel.source.internal.annotations.entity.EntityBindingContext;
import org.hibernate.metamodel.source.internal.annotations.entity.ManagedTypeMetadata;
import org.hibernate.metamodel.source.internal.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.metamodel.spi.AttributePath;
import org.hibernate.metamodel.spi.AttributeRole;

import org.jboss.jandex.DotName;

/**
 * Represents the most basic definition of a persistent attribute.  At
 * the "next level up" we categorize attributes as either:<ul>
 *     <li>singular - {@link SingularAttribute}</li>
 *     <li>plural - {@link PluralAttribute}</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public interface PersistentAttribute extends Comparable<PersistentAttribute> {
	/**
	 * Access the name of the attribute being modeled.
	 *
	 * @return The attribute name
	 */
	String getName();

	/**
	 * The nature (category) of the attribute being modeled.
	 *
	 * @return The attribute nature
	 */
	Nature getNature();

	/**
	 * The container (class) for the attribute
	 *
	 * @return The attribute container
	 */
	ManagedTypeMetadata getContainer();

	/**
	 * The member on the container that represents the attribute *as defined
	 * by AccessType*.  In other words this is the class member where we can
	 * look for annotations.  It is not necessarily the same as the backing
	 * member used to inject/extract values during runtime.
	 *
	 * @return The backing member
	 */
	MemberDescriptor getBackingMember();

	/**
	 * This is a unique name for the attribute within the entire mapping.
	 * Generally roles are rooted at an entity name.  Each path in the role
	 * is separated by hash (#) signs.
	 * <p/>
	 * Practically speaking, this is used to uniquely identify collection
	 * and embeddable mappings.
	 *
	 * @return The attribute role.
	 */
	AttributeRole getRole();

	/**
	 * This is a unique name for the attribute within a top-level container.
	 * Mainly this is a normalized name used to apply AttributeOverride and
	 * AssociationOverride annotations.
	 *
	 * @return The attribute path
	 */
	AttributePath getPath();

	/**
	 * Obtain the AccessType in use for this attribute.  The AccessType defines where
	 * to look for annotations and is never {@code null}
	 *
	 * @return The AccessType, never {@code null}.
	 */
	AccessType getAccessType();

	/**
	 * Obtain the runtime accessor strategy.  This defines how we inject and
	 * extract values to/from the attribute at runtime.
	 *
	 * @return The runtime accessor strategy
	 */
	String getAccessorStrategy();

	/**
	 * Is this attribute lazy?
	 * <p/>
	 * NOTE : Hibernate currently only really supports laziness for
	 * associations. But JPA metadata defines lazy for basic attributes
	 * too; so we report that info at the basic level
	 *
	 * @return Whether the attribute it lazy.
	 */
	boolean isLazy();

	/**
	 * Do changes in this attribute trigger optimistic locking checks?
	 *
	 * @return {@code true} (the default) indicates the attribute is included
	 * in optimistic locking; {@code false} indicates it is not.
	 */
	boolean isIncludeInOptimisticLocking();

	// ugh
	public PropertyGeneration getPropertyGeneration();

	public abstract boolean isOptional();

	public abstract boolean isInsertable();

	public abstract boolean isUpdatable();


	AttributeTypeResolver getHibernateTypeResolver();

	public EntityBindingContext getContext();

	/**
	 * An enum defining the nature (categorization) of a persistent attribute.
	 */
	public static enum Nature {
		BASIC( JPADotNames.BASIC ),
		EMBEDDED_ID( JPADotNames.EMBEDDED_ID ),
		EMBEDDED( JPADotNames.EMBEDDED ),
		ANY( HibernateDotNames.ANY ),
		ONE_TO_ONE( JPADotNames.ONE_TO_ONE ),
		MANY_TO_ONE( JPADotNames.MANY_TO_ONE ),
		ONE_TO_MANY( JPADotNames.ONE_TO_MANY ),
		MANY_TO_MANY( JPADotNames.MANY_TO_MANY ),
		MANY_TO_ANY( HibernateDotNames.MANY_TO_ANY ),
		ELEMENT_COLLECTION_BASIC( JPADotNames.ELEMENT_COLLECTION ),
		ELEMENT_COLLECTION_EMBEDDABLE( JPADotNames.ELEMENT_COLLECTION );

		private final DotName annotationDotName;

		Nature(DotName annotationDotName) {
			this.annotationDotName = annotationDotName;
		}

		public DotName getAnnotationDotName() {
			return annotationDotName;
		}
	}
}
