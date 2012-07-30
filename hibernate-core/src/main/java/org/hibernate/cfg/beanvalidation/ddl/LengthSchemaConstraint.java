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

import javax.validation.metadata.ConstraintDescriptor;
import javax.validation.metadata.PropertyDescriptor;

import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.BasicAttributeBinding;
import org.hibernate.metamodel.spi.binding.RelationalValueBinding;
import org.hibernate.metamodel.spi.relational.Value;

/**
 * @author Hardy Ferentschik
 */
public class LengthSchemaConstraint implements SchemaConstraint {
	// apply hibernate validator specific constraints - we cannot import any HV specific classes though!
	// no need to check explicitly for @Range. @Range is a composed constraint using @Min and @Max which
	// will be taken care later
	private static final String LENGTH_CONSTRAINT = "org.hibernate.validator.constraints.Length";

	@Override
	public boolean applyConstraint(Property property, ConstraintDescriptor<?> descriptor, PropertyDescriptor propertyDescriptor, Dialect dialect) {
		if ( !shouldConstraintBeApplied( descriptor, propertyDescriptor ) ) {
			return false;
		}

		int max = getMaxValue( descriptor );
		Column col = ( Column ) property.getColumnIterator().next();
		if ( max < Integer.MAX_VALUE ) {
			col.setLength( max );
		}
		return true;
	}

	@Override
	public boolean applyConstraint(AttributeBinding attributeBinding, ConstraintDescriptor<?> descriptor, PropertyDescriptor propertyDescriptor, Dialect dialect) {
		if ( !shouldConstraintBeApplied( descriptor, propertyDescriptor ) ) {
			return false;
		}

		int max = getMaxValue( descriptor );
		if ( !( attributeBinding instanceof BasicAttributeBinding ) ) {
			// TODO verify that's correct (HF)
			return false;
		}

		BasicAttributeBinding basicAttributeBinding = ( BasicAttributeBinding ) attributeBinding;
		RelationalValueBinding valueBinding = basicAttributeBinding.getRelationalValueBindings().get( 0 );
		Value value = valueBinding.getValue();

		if ( !( value instanceof org.hibernate.metamodel.spi.relational.Column ) ) {
			return false;
		}

		org.hibernate.metamodel.spi.relational.Column column = ( org.hibernate.metamodel.spi.relational.Column ) value;
		column.getSize().setLength( max );

		return true;
	}

	private int getMaxValue(ConstraintDescriptor<?> descriptor) {
		return ( Integer ) descriptor.getAttributes().get( "max" );
	}

	private boolean shouldConstraintBeApplied(ConstraintDescriptor<?> descriptor, PropertyDescriptor propertyDescriptor) {
		if ( !LENGTH_CONSTRAINT.equals( descriptor.getAnnotation().annotationType().getName() ) ) {
			return false;
		}

		if ( !String.class.equals( propertyDescriptor.getElementClass() ) ) {
			return false;
		}
		return true;
	}
}


