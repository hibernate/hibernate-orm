/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type;

import java.time.Year;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.java.YearJavaDescriptor;
import org.hibernate.type.descriptor.sql.IntegerTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#INTEGER INTEGER} and {@link java.time.Year}.
 *
 * @author Steve Ebersole
 */
public class YearType
		extends AbstractSingleColumnStandardBasicType<Year>
		implements LiteralType<Year> {

	/**
	 * Singleton access
	 */
	public static final YearType INSTANCE = new YearType();

	public YearType() {
		super( IntegerTypeDescriptor.INSTANCE, YearJavaDescriptor.INSTANCE );
	}

	@Override
	public String objectToSQLString(Year value, Dialect dialect) throws Exception {
		return String.valueOf( value.getValue() );
	}

	@Override
	public String getName() {
		return Year.class.getSimpleName();
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}
}
