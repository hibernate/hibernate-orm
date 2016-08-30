/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.type.descriptor.sql.UtcTimestampTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#TIMESTAMP TIMESTAMP} and {@link java.sql.Timestamp}
 * defaulting to UTC when no explicit time zone information is given
 *
 * @author Gavin King
 * @author Steve Ebersole
 * @author mirabilos
 */
public class UtcTimestampType extends TimestampType {
	public static final UtcTimestampType INSTANCE = new UtcTimestampType();

	public UtcTimestampType() {
		super();
		setSqlTypeDescriptor( UtcTimestampTypeDescriptor.INSTANCE );
	}
}
