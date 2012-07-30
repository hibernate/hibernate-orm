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

import javax.validation.constraints.Min;
import javax.validation.metadata.ConstraintDescriptor;
import javax.validation.metadata.PropertyDescriptor;

import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.spi.binding.AttributeBinding;

/**
 * @author Hardy Ferentschik
 */
public class MinSchemaConstraint implements SchemaConstraint {
	@Override
	public boolean applyConstraint(Property property, ConstraintDescriptor<?> descriptor, PropertyDescriptor propertyDescriptor, Dialect dialect) {
		if ( !Min.class.equals( descriptor.getAnnotation().annotationType() ) ) {
		  return false;
		}

		@SuppressWarnings("unchecked")
		ConstraintDescriptor<Min> minConstraint = ( ConstraintDescriptor<Min> ) descriptor;
		long min = minConstraint.getAnnotation().value();

		Column col = ( Column ) property.getColumnIterator().next();
		String checkConstraint = col.getQuotedName( dialect ) + ">=" + min;
		SchemaModificationHelper.applySQLCheck( col, checkConstraint );
		return true;


	}

	@Override
	public boolean applyConstraint(AttributeBinding attributeBinding, ConstraintDescriptor<?> descriptor, PropertyDescriptor propertyDescriptor, Dialect dialect) {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}
}


