/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.basic.BasicTypeImpl;
import org.hibernate.type.descriptor.java.internal.BooleanJavaDescriptor;
import org.hibernate.type.spi.descriptor.sql.CharTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#CHAR CHAR(1)} and {@link Boolean} (using 'T' and 'F')
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class TrueFalseType extends BasicTypeImpl<Boolean> {

	public static final TrueFalseType INSTANCE = new TrueFalseType();

	public TrueFalseType() {
		super( new BooleanJavaDescriptor( 'T', 'F' ), CharTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "true_false";
	}

	@Override
	public JdbcLiteralFormatter<Boolean> getJdbcLiteralFormatter() {
		return CharTypeDescriptor.INSTANCE.getJdbcLiteralFormatter( getJavaTypeDescriptor() );
	}
}
