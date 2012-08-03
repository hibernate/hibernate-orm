/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.cfg.beanvalidation.ddl;

import javax.validation.constraints.Size;
import javax.validation.metadata.ConstraintDescriptor;
import javax.validation.metadata.PropertyDescriptor;

import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.spi.binding.AttributeBinding;

/**
 * @author Hardy Ferentschik
 */
public class SizeSchemaConstraint implements SchemaConstraint {
	@Override
	public boolean applyConstraint(Property property, ConstraintDescriptor<?> descriptor, PropertyDescriptor propertyDescriptor, Dialect dialect) {
		if ( !shouldConstraintBeApplied( descriptor, propertyDescriptor ) ) {
			return false;
		}

		int max = SchemaModificationHelper.getValue( descriptor, "max", Integer.class );

		Column col = ( Column ) property.getColumnIterator().next();
		if ( max < Integer.MAX_VALUE ) {
			col.setLength( max );
			return true;
		}
		return false;
	}

	@Override
	public boolean applyConstraint(AttributeBinding attributeBinding, ConstraintDescriptor<?> descriptor, PropertyDescriptor propertyDescriptor, Dialect dialect) {
		if ( !shouldConstraintBeApplied( descriptor, propertyDescriptor ) ) {
			return false;
		}

		int max = SchemaModificationHelper.getValue( descriptor, "max", Integer.class );

		org.hibernate.metamodel.spi.relational.Column column = SchemaModificationHelper.getSingleColumn(  attributeBinding );
		if ( column == null ) {
			return false;
		}

		if ( max < Integer.MAX_VALUE ) {
			column.getSize().setLength( max );
			return true;
		}
		return false;
	}

	private boolean shouldConstraintBeApplied(ConstraintDescriptor<?> descriptor, PropertyDescriptor propertyDescriptor) {
		if ( !Size.class.equals( descriptor.getAnnotation().annotationType() ) ) {
			return false;
		}

		if ( !String.class.equals( propertyDescriptor.getElementClass() ) ) {
			return false;
		}

		return true;
	}
}


