package org.hibernate.type;

import jakarta.annotation.Nullable;

import org.hibernate.SPI;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
@SPI({ USE, IMPLEMENT })
public abstract class AbstractSingleColumnStandardBasicType<T>
		extends AbstractStandardBasicType<T>
		implements Type {

	@SPI(IMPLEMENT)
	public AbstractSingleColumnStandardBasicType(JdbcType jdbcType, JavaType<T> javaType) {
		super( jdbcType, javaType );
	}

	@Override
	public final void nullSafeSet(PreparedStatement st, @Nullable Object value, int index, boolean[] settable, SharedSessionContractImplementor session)
			throws HibernateException, SQLException {
		if ( settable[0] ) {
			nullSafeSet( st, value, index, session );
		}
	}
}
