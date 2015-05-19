/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.io.Serializable;
import java.util.Comparator;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.descriptor.java.IntegerTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#INTEGER INTEGER} and @link Integer}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class IntegerType extends AbstractSingleColumnStandardBasicType<Integer>
		implements PrimitiveType<Integer>, DiscriminatorType<Integer>, VersionType<Integer> {

	public static final IntegerType INSTANCE = new IntegerType();

	public static final Integer ZERO = 0;

	public IntegerType() {
		super( org.hibernate.type.descriptor.sql.IntegerTypeDescriptor.INSTANCE, IntegerTypeDescriptor.INSTANCE );
	}
	@Override
	public String getName() {
		return "integer";
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] { getName(), int.class.getName(), Integer.class.getName() };
	}
	@Override
	public Serializable getDefaultValue() {
		return ZERO;
	}
	@Override
	public Class getPrimitiveClass() {
		return int.class;
	}
	@Override
	public String objectToSQLString(Integer value, Dialect dialect) throws Exception {
		return toString( value );
	}
	@Override
	public Integer stringToObject(String xml) {
		return fromString( xml );
	}
	@Override
	public Integer seed(SessionImplementor session) {
		return ZERO;
	}
	@Override
	public Integer next(Integer current, SessionImplementor session) {
		return current+1;
	}
	@Override
	public Comparator<Integer> getComparator() {
		return getJavaTypeDescriptor().getComparator();
	}
}
