/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.type;

import java.io.Serializable;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.UserType;

import jakarta.inject.Singleton;
import org.assertj.core.util.Objects;

/**
 * @author Steve Ebersole
 */
@Singleton
public class UrlType implements UserType<URL> {
// because we cannot use CDI, injection is not available
//	private final OtherBean otherBean;
//
//	@Inject
//	public UrlType(OtherBean otherBean) {
//		if ( otherBean == null ) {
//			throw new UnsupportedOperationException( "OtherBean cannot be null" );
//		}
//		this.otherBean = otherBean;
//	}
//
//	public OtherBean getOtherBean() {
//		return otherBean;
//	}

	@Override
	public int getSqlType() {
		return SqlTypes.VARCHAR;
	}

	@Override
	public Class<URL> returnedClass() {
		return URL.class;
	}

	@Override
	public boolean equals(URL x, URL y) {
		return Objects.areEqual( x, y );
	}

	@Override
	public int hashCode(URL x) {
		return Objects.hashCodeFor( x );
	}

	@Override
	public URL nullSafeGet(ResultSet rs, int position, WrapperOptions options)
			throws SQLException {
		throw new UnsupportedOperationException( "Not used" );
	}

	@Override
	public void nullSafeSet(PreparedStatement st, URL value, int index, WrapperOptions options)
			throws SQLException {
		throw new UnsupportedOperationException( "Not used" );
	}

	@Override
	public URL deepCopy(URL value) {
		throw new UnsupportedOperationException( "Not used" );
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Serializable disassemble(URL value) {
		throw new UnsupportedOperationException( "Not used" );
	}

	@Override
	public URL assemble(Serializable cached, Object owner) {
		throw new UnsupportedOperationException( "Not used" );
	}
}
