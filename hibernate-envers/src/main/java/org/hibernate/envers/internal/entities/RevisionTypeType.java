package org.hibernate.envers.internal.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

import org.hibernate.HibernateException;
import org.hibernate.envers.RevisionType;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.UserType;

/**
 * A hibernate type for the {@link RevisionType} enum.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public class RevisionTypeType implements UserType<RevisionType>, Serializable {
	private static final long serialVersionUID = -1053201518229282688L;

	@Override
	public int getSqlType() {
		return Types.TINYINT;
	}

	@Override
	@Nonnull
	public Class<RevisionType> returnedClass() {
		return RevisionType.class;
	}

	@Override
	@Nullable
	public RevisionType nullSafeGet(@Nonnull ResultSet rs, int position, @Nonnull WrapperOptions options)
			throws SQLException {
		byte byteValue = rs.getByte( position );
		if ( rs.wasNull() ) {
			return null;
		}
		return RevisionType.fromRepresentation( byteValue );
	}

	@Override
	public void nullSafeSet(
			@Nonnull PreparedStatement preparedStatement,
			@Nullable RevisionType value,
			int index,
			@Nonnull WrapperOptions options)
			throws SQLException {
		if ( value == null ) {
			preparedStatement.setNull( index, Types.TINYINT );
		}
		else {
			preparedStatement.setByte( index, value.getRepresentation() );
		}
	}

	@Override
	@Nullable
	public RevisionType deepCopy(@Nullable RevisionType value) throws HibernateException {
		return value;
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	@Nullable
	public RevisionType assemble(@Nullable Serializable cached, @Nullable Object owner) throws HibernateException {
		return (RevisionType) cached;
	}

	@Override
	@Nullable
	public Serializable disassemble(@Nullable RevisionType value) throws HibernateException {
		return value;
	}

	@Override
	@Nullable
	public RevisionType replace(
			@Nullable RevisionType original,
			@Nullable RevisionType target,
			@Nullable Object owner) throws HibernateException {
		return original;
	}

	@Override
	public int hashCode(@Nonnull RevisionType x) throws HibernateException {
		return x.hashCode();
	}

	@Override
	public boolean equals(@Nullable RevisionType x, @Nullable RevisionType y) throws HibernateException {
		return Objects.equals( x, y );
	}
}
