package org.hibernate.usertype;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.BiConsumer;

import org.hibernate.HibernateException;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * @author Steve Ebersole
 */
@SPI({ USE, IMPLEMENT })
public abstract class BaseUserTypeSupport<T> implements UserType<T> {
	@SPI(IMPLEMENT)
	public BaseUserTypeSupport() {
	}

	private BasicJavaType<T> javaType;
	private JdbcType jdbcType;

	private boolean resolved;

	// cached JDBC extractor and binder
	private ValueExtractor<T> jdbcValueExtractor;
	private ValueBinder<T> jdbcValueBinder;

	protected abstract void resolve(@Nonnull BiConsumer<BasicJavaType<T>, JdbcType> resolutionConsumer);

	private void ensureResolved() {
		if ( !resolved ) {
			resolve( (javaType, jdbcType) -> {
				this.javaType = javaType;
				this.jdbcType = jdbcType;

				jdbcValueExtractor = jdbcType.getExtractor( javaType );
				jdbcValueBinder = jdbcType.getBinder( javaType );

				resolved = true;
			} );
		}
	}

	@Nonnull
	protected JdbcType jdbcType() {
		ensureResolved();
		return jdbcType;
	}

	@Nonnull
	protected BasicJavaType<T> javaType() {
		ensureResolved();
		return javaType;
	}

	@Override
	public int getSqlType() {
		ensureResolved();
		return jdbcType.getDdlTypeCode();
	}

	@Override
	@Nonnull
	public Class<T> returnedClass() {
		return javaType().getJavaTypeClass();
	}

	@Override
	public boolean equals(@Nullable T x, @Nullable T y) throws HibernateException {
		return javaType().areEqual( x, y );
	}

	@Override
	public int hashCode(@Nonnull T x) throws HibernateException {
		return javaType().extractHashCode( x );
	}

	@Override
	@Nullable
	public T nullSafeGet(@Nonnull ResultSet rs, int position, @Nonnull WrapperOptions options) throws SQLException {
		ensureResolved();
		return jdbcValueExtractor.extract( rs, position, options );
	}

	@Override
	public void nullSafeSet(
			@Nonnull PreparedStatement st,
			@Nullable T value,
			int index,
			@Nonnull WrapperOptions options) throws SQLException {
		ensureResolved();
		jdbcValueBinder.bind( st, value, index, options );
	}

	@Override
	@Nullable
	public T deepCopy(@Nullable T value) throws HibernateException {
		return javaType().getMutabilityPlan().deepCopy( value );
	}

	@Override
	public boolean isMutable() {
		return javaType().getMutabilityPlan().isMutable();
	}

	@Override
	@Nullable
	public Serializable disassemble(@Nullable T value) throws HibernateException {
		return javaType().getMutabilityPlan().disassemble( value, null );
	}

	@Override
	@Nullable
	public T assemble(@Nullable Serializable cached, @Nullable Object owner) throws HibernateException {
		return javaType().getMutabilityPlan().assemble( cached, null );
	}
}
