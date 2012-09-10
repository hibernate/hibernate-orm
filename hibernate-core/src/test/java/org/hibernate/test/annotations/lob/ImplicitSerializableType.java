package org.hibernate.test.annotations.lob;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.test.annotations.lob.EntitySerialize.CommonSerializable;
import org.hibernate.type.SerializableToBlobType;

/**
 * @author Janario Oliveira
 */
public class ImplicitSerializableType extends SerializableToBlobType {

	@Override
	public Object get(ResultSet rs, String name) throws SQLException {
		CommonSerializable deserialize = (CommonSerializable) super.get( rs, name );
		deserialize.setDefaultValue( "IMPLICIT" );
		return deserialize;
	}

	@Override
	public void set(PreparedStatement st, Object value, int index, SessionImplementor session) throws SQLException {
		if ( value != null ) {
			( (CommonSerializable) value ).setDefaultValue( null );
		}
		super.set( st, value, index, session );
	}

}
