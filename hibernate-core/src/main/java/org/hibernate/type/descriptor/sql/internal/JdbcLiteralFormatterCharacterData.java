/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.sql.internal;

import java.util.Locale;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.spi.BasicJdbcLiteralFormatter;

/**
 * JdbcLiteralFormatter implementation for handling character data
 *
 * @author Steve Ebersole
 */
public class JdbcLiteralFormatterCharacterData extends BasicJdbcLiteralFormatter {
	public static final String NATIONALIZED_PREFIX = "n";

	private final boolean isNationalized;

	public JdbcLiteralFormatterCharacterData(JavaTypeDescriptor javaTypeDescriptor) {
		this( javaTypeDescriptor, false );
	}

	public JdbcLiteralFormatterCharacterData(JavaTypeDescriptor javaTypeDescriptor, boolean isNationalized) {
		super( javaTypeDescriptor );
		this.isNationalized = isNationalized;
	}

	@Override
	public String toJdbcLiteral(Object value, Dialect dialect, SharedSessionContractImplementor session) {
		final String literalValue = unwrap( value, String.class, session );

		final String inlineLiteral = dialect.inlineLiteral( literalValue );

		if ( isNationalized ) {
			// is there a standardized form for n-string literals?  This is the SQL Server syntax for sure
			return NATIONALIZED_PREFIX.concat( inlineLiteral );
		}

		return inlineLiteral;
	}
}
