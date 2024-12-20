/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.binder.internal;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.Comment;
import org.hibernate.binder.AttributeBinder;
import org.hibernate.binder.TypeBinder;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;

/**
 * Handles {@link Comment} annotations.
 *
 * @author Gavin King
 */
public class CommentBinder implements AttributeBinder<Comment>, TypeBinder<Comment> {
	@Override
	public void bind(Comment comment, MetadataBuildingContext context, PersistentClass entity, Property property) {
		String text = comment.value();
		String on = comment.on();
		Value value = property.getValue();
		if ( value instanceof OneToMany ) {
			throw new AnnotationException( "One to many association '" + property.getName()
					+ "' was annotated '@Comment'");
		}
		else if ( value instanceof Collection ) {
			Collection collection = (Collection) value;
			Table table = collection.getTable();
			// by default, the comment goes on the table
			if ( on.isEmpty() || table.getName().equalsIgnoreCase( on ) ) {
				table.setComment( text );
			}
			// but if 'on' is explicit, it can go on a column
			Value element = collection.getElement();
			for ( Column column : element.getColumns() ) {
				if ( column.getName().equalsIgnoreCase( on ) ) {
					column.setComment( text );
				}
			}
			//TODO: list index / map key columns
		}
		else {
			for ( Column column : value.getColumns() ) {
				if ( on.isEmpty() || column.getName().equalsIgnoreCase( on ) ) {
					column.setComment( text );
				}
			}
		}
	}

	@Override
	public void bind(Comment comment, MetadataBuildingContext context, PersistentClass entity) {
		String text = comment.value();
		String on = comment.on();
		Table primary = entity.getTable();
		// by default, the comment goes on the primary table
		if ( on.isEmpty() || primary.getName().equalsIgnoreCase( on ) ) {
			primary.setComment( text );
		}
		// but if 'on' is explicit, it can go on a secondary table
		for ( Join join : entity.getJoins() ) {
			Table secondary = join.getTable();
			if ( secondary.getName().equalsIgnoreCase( on ) ) {
				secondary.setComment( text );
			}
		}
	}

	@Override
	public void bind(Comment comment, MetadataBuildingContext context, Component embeddable) {
		throw new AnnotationException( "Embeddable class '" + embeddable.getComponentClassName()
				+ "' was annotated '@Comment' (annotate its attributes instead)" );
	}
}
