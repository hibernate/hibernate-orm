/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.io.Serializable;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.java.BooleanTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#BOOLEAN BOOLEAN} and {@link Boolean}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class BooleanType
		extends AbstractSingleColumnStandardBasicType<Boolean>
		implements PrimitiveType<Boolean>, DiscriminatorType<Boolean> {
	public static final BooleanType INSTANCE = new BooleanType();

	public BooleanType() {
		this( org.hibernate.type.descriptor.sql.BooleanTypeDescriptor.INSTANCE, BooleanTypeDescriptor.INSTANCE );
	}

	protected BooleanType(SqlTypeDescriptor sqlTypeDescriptor, BooleanTypeDescriptor javaTypeDescriptor) {
		super( sqlTypeDescriptor, javaTypeDescriptor );
	}
	@Override
	public String getName() {
		return "boolean";
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] { getName(), boolean.class.getName(), Boolean.class.getName() };
	}
	@Override
	public Class getPrimitiveClass() {
		return boolean.class;
	}
	@Override
	public Serializable getDefaultValue() {
		return Boolean.FALSE;
	}
	@Override
	public Boolean stringToObject(String string) {
		return fromString( string );
	}

	@Override
	public String objectToSQLString(Boolean value, Dialect dialect) {
		return dialect.toBooleanValueString( value );
	}
}





