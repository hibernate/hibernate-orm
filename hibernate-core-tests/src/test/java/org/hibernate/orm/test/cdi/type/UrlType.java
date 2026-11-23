package org.hibernate.orm.test.cdi.type;

import java.io.Serializable;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.UserType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
	@Nonnull
	public Class<URL> returnedClass() {
		return URL.class;
	}

	@Override
	public boolean equals(@Nullable URL x, @Nullable URL y) {
		return Objects.areEqual( x, y );
	}

	@Override
	public int hashCode(@Nonnull URL x) {
		return Objects.hashCodeFor( x );
	}

	@Override
	@Nullable
	public URL nullSafeGet(@Nonnull ResultSet rs, int position, @Nonnull WrapperOptions options)
			throws SQLException {
		throw new UnsupportedOperationException( "Not used" );
	}

	@Override
	public void nullSafeSet(
			@Nonnull PreparedStatement st,
			@Nullable URL value,
			int index,
			@Nonnull WrapperOptions options)
			throws SQLException {
		throw new UnsupportedOperationException( "Not used" );
	}

	@Override
	@Nullable
	public URL deepCopy(@Nullable URL value) {
		throw new UnsupportedOperationException( "Not used" );
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	@Nullable
	public Serializable disassemble(@Nullable URL value) {
		throw new UnsupportedOperationException( "Not used" );
	}

	@Override
	@Nullable
	public URL assemble(@Nullable Serializable cached, @Nullable Object owner) {
		throw new UnsupportedOperationException( "Not used" );
	}
}
