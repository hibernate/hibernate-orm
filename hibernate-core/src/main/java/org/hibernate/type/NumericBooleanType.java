/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.BooleanJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.IntegerJdbcTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#INTEGER INTEGER} and {@link Boolean} (using 1 and 0)
 *
 * @author Steve Ebersole
 */
public class NumericBooleanType 
		extends AbstractSingleColumnStandardBasicType<Boolean>
		implements ConvertedBasicType<Boolean> {

	public static final NumericBooleanType INSTANCE = new NumericBooleanType();

	public NumericBooleanType() {
		super( IntegerJdbcTypeDescriptor.INSTANCE, BooleanJavaTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "numeric_boolean";
	}

	@Override
	public BasicValueConverter<Boolean, ?> getValueConverter() {
		return NumericBooleanConverter.INSTANCE;
	}

}
