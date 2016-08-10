/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.descriptor.java.ClassTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.VarcharTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#VARCHAR VARCHAR} and {@link Class}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class ClassType extends AbstractSingleColumnStandardBasicType<Class> implements JdbcLiteralFormatter<Class> {
	public static final ClassType INSTANCE = new ClassType();

	public ClassType() {
		super( VarcharTypeDescriptor.INSTANCE, ClassTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "class";
	}

	@Override
	public JdbcLiteralFormatter<Class> getJdbcLiteralFormatter() {
		return this;
	}

	@Override
	public String toJdbcLiteral(Class value, Dialect dialect) {
		return StringType.INSTANCE.toJdbcLiteral( toString( value ), dialect );
	}
}
