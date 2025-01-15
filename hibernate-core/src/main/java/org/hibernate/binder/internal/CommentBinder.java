/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
			Table table = collection.getCollectionTable();
			// by default, the comment goes on the table
			if ( on.isEmpty() || table.getName().equalsIgnoreCase( on ) ) {
				table.setComment( text );
			}
			else {
				// but if 'on' is explicit, it can go on a column
				for ( Column column : table.getColumns() ) {
					if ( column.getName().equalsIgnoreCase( on ) ) {
						column.setComment( text );
						return;
					}
				}
				throw new AnnotationException( "No matching column for '@Comment(on=\"" + on + "\")'" );
			}
		}
		else {
			for ( Column column : value.getColumns() ) {
				if ( on.isEmpty() || column.getName().equalsIgnoreCase( on ) ) {
					column.setComment( text );
					return;
				}
			}
			throw new AnnotationException( "No matching column for '@Comment(on=\"" + on + "\")'" );
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
		else {
			// but if 'on' is explicit, it can go on a secondary table
			for ( Join join : entity.getJoins() ) {
				Table secondary = join.getTable();
				if ( secondary.getName().equalsIgnoreCase( on ) ) {
					secondary.setComment( text );
					return;
				}
			}
			throw new AnnotationException( "No matching column for '@Comment(on=\"" + on + "\")'" );
		}
	}

	@Override
	public void bind(Comment comment, MetadataBuildingContext context, Component embeddable) {
		throw new AnnotationException( "Embeddable class '" + embeddable.getComponentClassName()
				+ "' was annotated '@Comment' (annotate its attributes instead)" );
	}
}
