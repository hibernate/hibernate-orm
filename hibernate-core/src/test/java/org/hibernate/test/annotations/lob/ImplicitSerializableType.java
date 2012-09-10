package org.hibernate.test.annotations.lob;

import java.io.Serializable;

import org.hibernate.type.SerializableToBlobType;

/**
 * @author Janario Oliveira
 */
public class ImplicitSerializableType<T extends Serializable> extends SerializableToBlobType<T> {

	// TODO: Find another way to test that this type is being used by
	// SerializableToBlobTypeTest#testPersist.  Most AbstractStandardBasicType
	// methods are final.
	
//	@Override
//	public Object get(ResultSet rs, String name) throws SQLException {
//		CommonSerializable deserialize = (CommonSerializable) super.get( rs, name );
//		deserialize.setDefaultValue( "IMPLICIT" );
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
