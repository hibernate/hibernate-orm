/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.converter;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Locale;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.PersistenceException;

import org.hibernate.Remove;
import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.query.sqm.CastType;
import org.hibernate.sql.ast.SqlTreeCreationException;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.TrueFalseConverter;
import org.hibernate.type.YesNoConverter;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import org.jboss.logging.Logger;

/**
 * Adapter for incorporating JPA {@link AttributeConverter} handling into the JdbcType contract.
 * <p/>
 * Essentially this is responsible for mapping to/from the intermediate database type representation.  Continuing the
 * {@code AttributeConverter<Integer,String>} example from
 * {@link org.hibernate.mapping.SimpleValue#buildAttributeConverterTypeAdapter()}, the "intermediate database type
 * representation" would be the String representation.  So on binding, we convert the incoming Integer to String;
 * on extraction we extract the value as String and convert to Integer.
 *
 * @author Steve Ebersole
 * @deprecated remove
 */
@Remove
@Deprecated(forRemoval = true)
@SuppressWarnings("JavadocReference")
public class AttributeConverterJdbcTypeAdapter implements JdbcType {
	private static final Logger log = Logger.getLogger( AttributeConverterJdbcTypeAdapter.class );

	private final BasicValueConverter converter;
	private final JdbcType delegate;
	private final JavaType intermediateJavaType;

	public AttributeConverterJdbcTypeAdapter(
			BasicValueConverter converter,
			JdbcType delegate,
			JavaType intermediateJavaType) {
		this.converter = converter;
		this.delegate = delegate;
		this.intermediateJavaType = intermediateJavaType;
	}

	@Override
	public int getJdbcTypeCode() {
		return delegate.getJdbcTypeCode();
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return delegate.getDefaultSqlTypeCode();
	}

	@Override
	public String toString() {
		return "AttributeConverterJdbcTypeAdapter(" + converter.getClass().getName() + ")";
	}

	@Override
	public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
		return delegate.getPreferredJavaTypeClass( options );
	}

	public JdbcType getUnderlyingJdbcType() {
		return delegate;
	}

	@Override
	public CastType getCastType() {
		if ( converter.getDomainJavaType().getJavaType() == Boolean.class
				&& converter.getRelationalJavaType().getJavaType() == Character.class ) {
			switch ( delegate.getJdbcTypeCode() ) {
				case SqlTypes.CHAR:
				case SqlTypes.NCHAR:
					if ( converter instanceof TrueFalseConverter ) {
						return CastType.TF_BOOLEAN;
					}
					else if ( converter instanceof YesNoConverter ) {
						return CastType.YN_BOOLEAN;
					}
			}
		}
		return JdbcType.super.getCastType();
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		final JdbcLiteralFormatter<Object> jdbcLiteralFormatter = delegate.getJdbcLiteralFormatter( intermediateJavaType );
		if ( jdbcLiteralFormatter == null ) {
			return null;
		}
		return new JdbcLiteralFormatter<T>() {
			@Override
			public void appendJdbcLiteral(
					SqlAppender appender,
					T value,
					Dialect dialect,
					WrapperOptions wrapperOptions) {
				final Object convertedValue;
				if ( value == null || converter.getDomainJavaType().getJavaTypeClass().isInstance( value ) ) {
					try {
						convertedValue = converter.toRelationalValue( value );
					}
					catch (PersistenceException pe) {
						throw pe;
					}
					catch (RuntimeException re) {
						throw new PersistenceException( "Error attempting to apply AttributeConverter", re );
					}
				}
				else if ( converter.getRelationalJavaType().getJavaTypeClass().isInstance( value ) ) {
					convertedValue = value;
				}
				else {
					throw new SqlTreeCreationException(
							String.format(
									Locale.ROOT,
									"Literal type [`%s`] did not match domain Java-type [`%s`] nor JDBC Java-type [`%s`]",
									value.getClass(),
									converter.getDomainJavaType().getJavaTypeClass().getName(),
									converter.getRelationalJavaType().getJavaTypeClass().getName()
							)
					);
				}
				jdbcLiteralFormatter.appendJdbcLiteral( appender, convertedValue, dialect, wrapperOptions );
			}
		};
	}

	// Binding ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	@SuppressWarnings("unchecked")
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		// Get the binder for the intermediate type representation
		final ValueBinder realBinder = delegate.getBinder( intermediateJavaType );

		return new ValueBinder<X>() {
			@Override
			public void bind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
				final Object convertedValue;
				try {
					convertedValue = converter.toRelationalValue( value );
				}
				catch (PersistenceException pe) {
					throw pe;
				}
				catch (RuntimeException re) {
					throw new PersistenceException( "Error attempting to apply AttributeConverter", re );
				}

				log.debugf( "Converted value on binding : %s -> %s", value, convertedValue );
				realBinder.bind( st, convertedValue, index, options );
			}

			@Override
			public void bind(CallableStatement st, X value, String name, WrapperOptions options) throws SQLException {
				final Object convertedValue;
				try {
					convertedValue = converter.toRelationalValue( value );
				}
				catch (PersistenceException pe) {
					throw pe;
				}
				catch (RuntimeException re) {
					throw new PersistenceException( "Error attempting to apply AttributeConverter", re );
				}

				log.debugf( "Converted value on binding : %s -> %s", value, convertedValue );
				realBinder.bind( st, convertedValue, name, options );
			}
		};
	}


	// Extraction ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		assert javaType == intermediateJavaType
				|| javaType == converter.getDomainJavaType();
		return delegate.getExtractor( intermediateJavaType );
//		// Get the extractor for the intermediate type representation
//		final ValueExtractor realExtractor = delegate.getExtractor( intermediateJavaType );
//
//		return new ValueExtractor<X>() {
//			@Override
//			public X extract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
//				return doConversion( realExtractor.extract( rs, paramIndex, options ) );
//			}
//
//			@Override
//			public X extract(CallableStatement statement, int paramIndex, WrapperOptions options) throws SQLException {
//				return doConversion( realExtractor.extract( statement, paramIndex, options ) );
//			}
//
//			@Override
//			public X extract(CallableStatement statement, String paramName, WrapperOptions options) throws SQLException {
//				return doConversion( realExtractor.extract( statement, paramName, options ) );
//			}
//
//			@SuppressWarnings("unchecked")
//			private X doConversion(Object extractedValue) {
//				try {
//					X convertedValue = (X) converter.toDomainValue( extractedValue );
//					log.debugf( "Converted value on extraction: %s -> %s", extractedValue, convertedValue );
//					return convertedValue;
//				}
//				catch (PersistenceException pe) {
//					throw pe;
//				}
//				catch (RuntimeException re) {
//					throw new PersistenceException( "Error attempting to apply AttributeConverter", re );
//				}
//			}
//		};
	}
}
