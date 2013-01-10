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

import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.BasicAttributeBinding;
import org.hibernate.metamodel.spi.binding.RelationalValueBinding;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.Value;

/**
 * @author Hardy Ferentschik
 */
public class SchemaModificationHelper {
	private SchemaModificationHelper() {
	}

	public static String buildSQLCheck(String existingCheckCondition, String checkConstraint) {
		String newCondition;

		// need to check whether the new check is already part of the existing check, because applyDDL can be called
		// multiple times
		if ( StringHelper.isNotEmpty( existingCheckCondition ) && !existingCheckCondition.contains( checkConstraint ) ) {
			newCondition = existingCheckCondition + " AND " + checkConstraint;
		}
		else {
			newCondition = checkConstraint;
		}

		return newCondition;
	}

	public static Column getSingleColumn(AttributeBinding attributeBinding) {
		if ( !( attributeBinding instanceof SingularAttributeBinding ) ) {
			// TODO verify that's correct (HF)
			return null;
		}

		SingularAttributeBinding basicAttributeBinding = ( SingularAttributeBinding ) attributeBinding;
		RelationalValueBinding valueBinding = basicAttributeBinding.getRelationalValueBindings().get( 0 );
		Value value = valueBinding.getValue();

		if ( !( value instanceof Column ) ) {
			return null;
		}

		return ( Column ) value;
	}

	public static <T> T getValue(ConstraintDescriptor<?> descriptor, String parameterName, Class<T> type) {
		return ( T ) descriptor.getAttributes().get( parameterName );
	}
}


