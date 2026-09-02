/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

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
	public final void nullSafeSet(PreparedStatement st, Object value, int index, boolean[] settable, SharedSessionContractImplementor session)
			throws HibernateException, SQLException {
		if ( settable[0] ) {
			nullSafeSet( st, value, index, session );
		}
	}
}
