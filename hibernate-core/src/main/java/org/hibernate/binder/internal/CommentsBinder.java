/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.binder.internal;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.Comments;
import org.hibernate.binder.AttributeBinder;
import org.hibernate.binder.TypeBinder;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

import java.util.HashSet;
import java.util.Set;

/**
 * Handles the {@link Comments} annotation.
 *
 * @author Gavin King
 */
public class CommentsBinder implements TypeBinder<Comments>, AttributeBinder<Comments> {
	@Override
	public void bind(Comments comments, MetadataBuildingContext context, PersistentClass entity, Property property) {
		final var commentBinder = new CommentBinder();
		final Set<String> ons = new HashSet<>( comments.value().length );
		for ( var comment : comments.value() ) {
			if ( !ons.add( comment.on() ) ) {
				throw new AnnotationException( "Multiple '@Comment' annotations of '"
						+ property.getName() + "' had the same 'on'" );
			}
			commentBinder.bind( comment, context, entity, property );
		}
	}

	@Override
	public void bind(Comments comments, MetadataBuildingContext context, PersistentClass entity) {
		final var commentBinder = new CommentBinder();
		final Set<String> ons = new HashSet<>( comments.value().length );
		for ( var comment : comments.value() ) {
			if ( !ons.add( comment.on() ) ) {
				throw new AnnotationException( "Multiple '@Comment' annotations of entity '"
						+ entity.getEntityName() + "' had the same 'on'" );
			}
			commentBinder.bind( comment, context, entity );
		}
	}

	@Override
	public void bind(Comments comments, MetadataBuildingContext context, Component embeddable) {
		throw new AnnotationException( "Embeddable class '" + embeddable.getComponentClassName()
				+ "' was annotated '@Comment' (annotate its attributes instead)" );
	}
}
