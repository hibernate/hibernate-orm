package org.hibernate.type.descriptor;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Extracts values from a JDBC [ResultSet] or the output parameters of a
/// [CallableStatement].
///
/// Providers normally supply an extractor from
/// [org.hibernate.type.descriptor.jdbc.JdbcType#getExtractor(org.hibernate.type.descriptor.java.JavaType)].
/// An extractor must perform JDBC extraction only. It must not apply a
/// [org.hibernate.type.descriptor.converter.spi.BasicValueConverter]; the
/// caller coordinates conversion after extraction.
///
/// @param <X> the Java value type produced by this extractor
/// @author Steve Ebersole
/// @see org.hibernate.type.descriptor.jdbc.JdbcType#getExtractor(org.hibernate.type.descriptor.java.JavaType)
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface ValueExtractor<X> {
	/**
	 * Extract value from result set
	 *
	 * @throws SQLException Indicates a JDBC error occurred.
	 */
	@Nullable
	X extract(@Nonnull ResultSet rs, int paramIndex, @Nonnull WrapperOptions options) throws SQLException;

	/**
	 * Extract value from a callable output parameter by index
	 *
	 * @throws SQLException Indicates a JDBC error occurred.
	 */
	@Nullable
	X extract(
			@Nonnull CallableStatement statement,
			int paramIndex,
			@Nonnull WrapperOptions options) throws SQLException;

	/**
	 * Extract value from a callable output parameter by name
	 *
	 * @throws SQLException Indicates a JDBC error occurred.
	 */
	@Nullable
	X extract(
			@Nonnull CallableStatement statement,
			@Nonnull String paramName,
			@Nonnull WrapperOptions options) throws SQLException;
}
