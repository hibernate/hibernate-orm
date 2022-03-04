/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.sql.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.SqlTypes;

public class Scale6IntervalSecondDdlType extends DdlTypeImpl {

	public Scale6IntervalSecondDdlType(Dialect dialect) {
		this( "interval second($s)", dialect );
	}

	public Scale6IntervalSecondDdlType(String typeNamePattern, Dialect dialect) {
		super( SqlTypes.INTERVAL_SECOND, typeNamePattern, dialect );
	}
	@Override
	public String getTypeName(Long size, Integer precision, Integer scale) {
		// The maximum scale for `interval second` is 6 unfortunately so we have to use numeric by default
		if ( scale == null || scale > 6 ) {
			return DdlTypeImpl.replace( "numeric($p,$s)", size, precision, scale );
		}
		return super.getTypeName( size, precision, scale );
	}
}
