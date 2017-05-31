/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities;

import org.hibernate.envers.RevisionType;
import org.hibernate.type.descriptor.sql.spi.TinyIntSqlDescriptor;
import org.hibernate.type.internal.BasicTypeImpl;

/**
 * A hibernate type for the {@link RevisionType} enum.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class RevisionTypeType extends BasicTypeImpl<RevisionType> {
	public static final RevisionTypeType INSTANCE = new RevisionTypeType();

	public RevisionTypeType() {
		super( RevisionTypeJavaDescriptor.INSTANCE, TinyIntSqlDescriptor.INSTANCE );
	}

}
