package org.hibernate.orm.test.hql;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.EnhancedUserType;

/**
 * A custom type for mapping {@link Classification} instances
 * to the respective db column.
 * </p>
 * THis is largely intended to mimic JDK5 enum support in JPA.  Here we are
 * using the approach of storing the ordinal values, rather than the names.
 *
 * @author Steve Ebersole
 */
public class ClassificationType implements EnhancedUserType<Classification>, ValueExtractor<Classification> {

	@Override
	public int getSqlType() {
		return Types.TINYINT;
	}

	@Override
	@Nonnull
	public Class<Classification> returnedClass() {
		return Classification.class;
	}

	@Override
	public boolean equals(@Nullable Classification x, @Nullable Classification y) throws HibernateException {
		if ( x == null && y == null ) {
			return false;
		}
		else if ( x != null ) {
			return x.equals( y );
		}
		else {
			return y.equals( x );
		}
	}

	@Override
	public int hashCode(@Nonnull Classification x) throws HibernateException {
		return x.hashCode();
	}

	@Override
	@Nullable
	public Classification nullSafeGet(@Nonnull ResultSet rs, int position, @Nonnull WrapperOptions options)
			throws SQLException {
		final int intValue = rs.getInt( position );
		if ( rs.wasNull() ) {
			return null;
		}
		return Classification.valueOf( intValue );
	}

	@Override
	public void nullSafeSet(
			@Nonnull PreparedStatement st,
			@Nullable Classification value,
			int index,
			@Nonnull WrapperOptions options)
			throws SQLException {
		if ( value == null ) {
			st.setNull( index, Types.INTEGER );
		}
		else {
			st.setInt( index, value.ordinal() );
		}
	}

	@Override
	@Nullable
	public Classification deepCopy(@Nullable Classification value) throws HibernateException {
		return value;
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	@Nullable
	public Serializable disassemble(@Nullable Classification value) throws HibernateException {
		return ( Classification ) value;
	}

	@Override
	@Nullable
	public Classification assemble(@Nullable Serializable cached, @Nullable Object owner) throws HibernateException {
		return (Classification) cached;
	}

	@Override
	@Nullable
	public Classification replace(
			@Nullable Classification original,
			@Nullable Classification target,
			@Nullable Object owner) throws HibernateException {
		return original;
	}

	@Override
	public String toSqlLiteral(Classification value) {
		return Integer.toString( value.ordinal() );
	}

	@Override
	public String toString(Classification value) throws HibernateException {
		return value.name();
	}

	@Override
	public Classification fromStringValue(CharSequence sequence) {
		return Classification.valueOf( sequence.toString() );
	}

	@Override
	@Nullable
	public Classification extract(
			@Nonnull ResultSet rs,
			int paramIndex,
			@Nonnull WrapperOptions options) throws SQLException {
		return Classification.valueOf( rs.getInt( paramIndex ) );
	}

	@Override
	@Nullable
	public Classification extract(
			@Nonnull CallableStatement statement,
			int paramIndex,
			@Nonnull WrapperOptions options) throws SQLException {
		return Classification.valueOf( statement.getInt( paramIndex ) );
	}

	@Override
	@Nullable
	public Classification extract(
			@Nonnull CallableStatement statement,
			@Nonnull String paramName,
			@Nonnull WrapperOptions options) throws SQLException {
		return Classification.valueOf( statement.getInt( paramName ) );
	}
}
