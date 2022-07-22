/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.BasicCollectionJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * A type that maps between {@link java.sql.Types#ARRAY ARRAY} and {@code Collection<T>}
 *
 * @author Christian Beikov
 */
public class BasicCollectionType<C extends Collection<E>, E>
		extends AbstractSingleColumnStandardBasicType<C>
		implements AdjustableBasicType<C>, BasicPluralType<C, E> {

	private final BasicType<E> baseDescriptor;
	private final String name;
	private final ValueBinder<C> jdbcValueBinder;
	private final ValueExtractor<C> jdbcValueExtractor;

	public BasicCollectionType(BasicType<E> baseDescriptor, JdbcType arrayJdbcType, BasicCollectionJavaType<C, E> collectionTypeDescriptor) {
		super( arrayJdbcType, collectionTypeDescriptor );
		this.baseDescriptor = baseDescriptor;
		this.name = determineName( collectionTypeDescriptor, baseDescriptor );
		final ValueBinder<C> jdbcValueBinder = super.getJdbcValueBinder();
		final ValueExtractor<C> jdbcValueExtractor = super.getJdbcValueExtractor();
		//noinspection unchecked
		final BasicValueConverter<E, Object> valueConverter = (BasicValueConverter<E, Object>) baseDescriptor.getValueConverter();
		if ( valueConverter != null ) {
			this.jdbcValueBinder = new ValueBinder<C>() {
				@Override
				public void bind(PreparedStatement st, C value, int index, WrapperOptions options)
						throws SQLException {
					jdbcValueBinder.bind( st, getValue( value, valueConverter, options ), index, options );
				}

				@Override
				public void bind(CallableStatement st, C value, String name, WrapperOptions options)
						throws SQLException {
					jdbcValueBinder.bind( st, getValue( value, valueConverter, options ), name, options );
				}

				private C getValue(
						C value,
						BasicValueConverter<E, Object> valueConverter,
						WrapperOptions options) {
					if ( value == null ) {
						return null;
					}
					final JdbcType elementJdbcType = baseDescriptor.getJdbcType();
					final TypeConfiguration typeConfiguration = options.getSessionFactory().getTypeConfiguration();
					final JdbcType underlyingJdbcType = typeConfiguration.getJdbcTypeRegistry()
							.getDescriptor( elementJdbcType.getDefaultSqlTypeCode() );
					final Class<?> preferredJavaTypeClass = underlyingJdbcType.getPreferredJavaTypeClass( options );
					final Class<?> elementJdbcJavaTypeClass;
					if ( preferredJavaTypeClass == null ) {
						elementJdbcJavaTypeClass = underlyingJdbcType.getJdbcRecommendedJavaTypeMapping(
								null,
								null,
								typeConfiguration
						).getJavaTypeClass();
					}
					else {
						elementJdbcJavaTypeClass = preferredJavaTypeClass;
					}

					//noinspection unchecked
					final Collection<Object> converted = (Collection<Object>) collectionTypeDescriptor.getSemantics()
							.instantiateRaw( value.size(), null );
					for ( E element : value ) {
						converted.add(
								valueConverter.getRelationalJavaType().unwrap(
										valueConverter.toRelationalValue( element ),
										elementJdbcJavaTypeClass,
										options
								)
						);
					}
					//noinspection unchecked
					return (C) converted;
				}
			};
			this.jdbcValueExtractor = new ValueExtractor<C>() {
				@Override
				public C extract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
					return getValue( jdbcValueExtractor.extract( rs, paramIndex, options ), valueConverter );
				}

				@Override
				public C extract(CallableStatement statement, int paramIndex, WrapperOptions options)
						throws SQLException {
					return getValue( jdbcValueExtractor.extract( statement, paramIndex, options ), valueConverter );
				}

				@Override
				public C extract(CallableStatement statement, String paramName, WrapperOptions options)
						throws SQLException {
					return getValue( jdbcValueExtractor.extract( statement, paramName, options ), valueConverter );
				}

				private C getValue(C value, BasicValueConverter<E, Object> valueConverter) {
					if ( value == null ) {
						return null;
					}
					final C converted = collectionTypeDescriptor.getSemantics()
							.instantiateRaw( value.size(), null );
					for ( E element : value ) {
						converted.add( valueConverter.toDomainValue( element ) );
					}
					return converted;
				}
			};
		}
		else {
			this.jdbcValueBinder = jdbcValueBinder;
			this.jdbcValueExtractor = jdbcValueExtractor;
		}
	}

	private static String determineName(BasicCollectionJavaType<?, ?> collectionTypeDescriptor, BasicType<?> baseDescriptor) {
		switch ( collectionTypeDescriptor.getSemantics().getCollectionClassification() ) {
			case BAG:
			case ID_BAG:
				return "Collection<" + baseDescriptor.getName() + ">";
			case LIST:
				return "List<" + baseDescriptor.getName() + ">";
			case SET:
				return "Set<" + baseDescriptor.getName() + ">";
			case SORTED_SET:
				return "SortedSet<" + baseDescriptor.getName() + ">";
			case ORDERED_SET:
				return "OrderedSet<" + baseDescriptor.getName() + ">";
		}
		return null;
	}

	@Override
	public BasicType<E> getElementType() {
		return baseDescriptor;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	@Override
	public ValueExtractor<C> getJdbcValueExtractor() {
		return jdbcValueExtractor;
	}

	@Override
	public ValueBinder<C> getJdbcValueBinder() {
		return jdbcValueBinder;
	}

	@Override
	public <X> BasicType<X> resolveIndicatedType(JdbcTypeIndicators indicators, JavaType<X> domainJtd) {
		// TODO: maybe fallback to some encoding by default if the DB doesn't support arrays natively?
		//  also, maybe move that logic into the ArrayJdbcType
		//noinspection unchecked
		return (BasicType<X>) this;
	}
}
