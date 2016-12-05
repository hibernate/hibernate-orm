/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.util.TimeZone;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.basic.BasicTypeImpl;
import org.hibernate.type.spi.descriptor.java.TimeZoneTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.VarcharTypeDescriptor;

/**
 * A type mapping {@link java.sql.Types#VARCHAR VARCHAR} and {@link TimeZone}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class TimeZoneType
		extends BasicTypeImpl<TimeZone> implements JdbcLiteralFormatter<TimeZone> {

	public static final TimeZoneType INSTANCE = new TimeZoneType();

	public TimeZoneType() {
		super( TimeZoneTypeDescriptor.INSTANCE, VarcharTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "timezone";
	}

	@Override
	public JdbcLiteralFormatter<TimeZone> getJdbcLiteralFormatter() {
		return this;
	}

	@Override
	public String toJdbcLiteral(TimeZone value, Dialect dialect) {
		return StringType.INSTANCE.toJdbcLiteral( value.getID(), dialect );
	}
}
