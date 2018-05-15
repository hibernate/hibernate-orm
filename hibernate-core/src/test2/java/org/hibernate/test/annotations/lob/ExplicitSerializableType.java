/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.lob;

import java.io.Serializable;

import org.hibernate.type.SerializableToBlobType;

/**
 * @author Janario Oliveira
 */
public class ExplicitSerializableType<T extends Serializable> extends SerializableToBlobType<T> {
	
	// TODO: Find another way to test that this type is being used by
	// SerializableToBlobTypeTest#testPersist.  Most AbstractStandardBasicType
	// methods are final.
	
//	@Override
//	public Object get(ResultSet rs, String name) throws SQLException {
//		CommonSerializable deserialize = (CommonSerializable) super.get( rs, name );
//		deserialize.setDefaultValue( "EXPLICIT" );
//		return deserialize;
//	}
//
//	@Override
//	public void set(PreparedStatement st, Object value, int index, SessionImplementor session) throws SQLException {
//		if ( value != null ) {
//			( (CommonSerializable) value ).setDefaultValue( null );
//		}
//		super.set( st, value, index, session );
//	}

}
