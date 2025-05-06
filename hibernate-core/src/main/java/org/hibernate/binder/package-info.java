/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * 	This package defines an easy way to extend Hibernate with user-defined
 * 	annotations that define customized O/R mappings of annotated entities
 * 	and annotated entity attributes.
 * 	<ul>
 *	<li>The meta-annotation
 *	    {@link org.hibernate.annotations.TypeBinderType @TypeBinderType}
 * 	    associates a {@link org.hibernate.binder.TypeBinder} with a
 * 	    user-written annotation which targets entity and embeddable
 * 	    {@linkplain java.lang.annotation.ElementType#TYPE types}.
 *	<li>The meta-annotation
 *	    {@link org.hibernate.annotations.AttributeBinderType @AttributeBinderType}
 * 	    associates an {@link org.hibernate.binder.AttributeBinder}
 * 	    with a user-written annotation which targets
 * 	    {@linkplain java.lang.annotation.ElementType#FIELD fields} and
 * 	    properties of entity types and embeddable classes.
 * </ul>
 *
 * @see org.hibernate.binder.AttributeBinder
 * @see org.hibernate.binder.TypeBinder
 */
package org.hibernate.binder;
