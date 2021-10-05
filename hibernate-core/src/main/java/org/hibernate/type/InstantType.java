/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.hibernate.type.descriptor.java.InstantJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.TimestampJdbcTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#TIMESTAMP TIMESTAMP} and {@link java.time.LocalDateTime}.
 *
 * @author Steve Ebersole
 */
public class InstantType
		extends AbstractSingleColumnStandardBasicType<Instant> {
	/**
	 * Singleton access
	 */
	public static final InstantType INSTANCE = new InstantType();

	public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern( "yyyy-MM-dd HH:mm:ss.S 'Z'", Locale.ENGLISH );

	public InstantType() {
		super( TimestampJdbcTypeDescriptor.INSTANCE, InstantJavaTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "instant";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

}
