/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.lang.reflect.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Locale;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.NamedAuxiliaryDatabaseObject;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;

import oracle.jdbc.OracleConnection;

import static java.sql.Types.ARRAY;
import static java.util.Collections.emptySet;
import static org.hibernate.internal.util.collections.ArrayHelper.EMPTY_STRING_ARRAY;

/**
 * Descriptor for {@link Types#ARRAY ARRAY} handling.
 *
 * @author Christian Beikov
 * @author Jordan Gigov
 */
public class OracleArrayJdbcType extends ArrayJdbcType {

	private final String typeName;

	public OracleArrayJdbcType(JdbcType elementJdbcType, String typeName) {
		super( elementJdbcType );
		this.typeName = typeName;
	}

	public String getTypeName() {
		return typeName;
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaTypeDescriptor) {
		return null;
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaType<X> javaTypeDescriptor) {
		//noinspection unchecked
		final BasicPluralJavaType<X> containerJavaType = (BasicPluralJavaType<X>) javaTypeDescriptor;
		return new BasicBinder<>( javaTypeDescriptor, this ) {
			private String typeName(WrapperOptions options) {
				return ( typeName == null ? getTypeName( options, containerJavaType ) : typeName )
						.toUpperCase(Locale.ROOT);
			}
			@Override
			protected void doBindNull(PreparedStatement st, int index, WrapperOptions options) throws SQLException {
				st.setNull( index, ARRAY, typeName( options ) );
			}

			@Override
			protected void doBindNull(CallableStatement st, String name, WrapperOptions options) throws SQLException {
				st.setNull( name, ARRAY, typeName( options ) );
			}

			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
				st.setArray( index, getArray( value, containerJavaType, options ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final java.sql.Array arr = getArray( value, containerJavaType, options );
				try {
					st.setObject( name, arr, ARRAY );
				}
				catch (SQLException ex) {
					throw new HibernateException( "JDBC driver does not support named parameters for setArray. Use positional.", ex );
				}
			}

			private java.sql.Array getArray(X value, BasicPluralJavaType<X> containerJavaType, WrapperOptions options)
					throws SQLException {
				//noinspection unchecked
				final Class<Object[]> arrayClass = (Class<Object[]>) Array.newInstance(
						getElementJdbcType().getPreferredJavaTypeClass( options ),
						0
				).getClass();
				final Object[] objects = javaTypeDescriptor.unwrap( value, arrayClass, options );
				final String arrayTypeName = typeName( options ).toUpperCase(Locale.ROOT);

				final OracleConnection oracleConnection = options.getSession()
						.getJdbcCoordinator().getLogicalConnection().getPhysicalConnection()
						.unwrap( OracleConnection.class );
				try {
					return oracleConnection.createOracleArray( arrayTypeName, objects );
				}
				catch (Exception e) {
					throw new HibernateException( "Couldn't create a java.sql.Array", e );
				}
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaTypeDescriptor) {
		return new BasicExtractor<>( javaTypeDescriptor, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( rs.getArray( paramIndex ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( statement.getArray( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( statement.getArray( name ), options );
			}
		};
	}

	static String getTypeName(WrapperOptions options, BasicPluralJavaType<?> containerJavaType) {
		Dialect dialect = options.getSessionFactory().getJdbcServices().getDialect();
		return getTypeName( containerJavaType.getElementJavaType(), dialect );
	}

	static String getTypeName(JavaType<?> elementJavaType, Dialect dialect) {
		return dialect.getArrayTypeName(
				elementJavaType.getJavaTypeClass().getSimpleName(),
				null, // not needed by OracleDialect.getArrayTypeName()
				null // not needed by OracleDialect.getArrayTypeName()
		);
	}

	@Override
	public void addAuxiliaryDatabaseObjects(
			JavaType<?> javaType,
			Size columnSize,
			Database database,
			TypeConfiguration typeConfiguration) {
		final Dialect dialect = database.getDialect();
		final BasicPluralJavaType<?> pluralJavaType = (BasicPluralJavaType<?>) javaType;
		final JavaType<?> elementJavaType = pluralJavaType.getElementJavaType();
		final String arrayTypeName = typeName == null ? getTypeName( elementJavaType, dialect ) : typeName;
		final String elementType =
				typeConfiguration.getDdlTypeRegistry().getTypeName(
						getElementJdbcType().getDdlTypeCode(),
						dialect.getSizeStrategy().resolveSize(
								getElementJdbcType(),
								elementJavaType,
								columnSize.getPrecision(),
								columnSize.getScale(),
								columnSize.getLength()
						),
						new BasicTypeImpl<>( elementJavaType, getElementJdbcType() )
				);
		int arrayLength = columnSize.getArrayLength() == null ? 127 : columnSize.getArrayLength();
		database.addAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						arrayTypeName,
						database.getDefaultNamespace(),
						new String[]{
								"create or replace type " + arrayTypeName
										+ " as varying array(" + arrayLength + ") of " + elementType
						},
						new String[] { "drop type " + arrayTypeName + " force" },
						emptySet(),
						true
				)
		);
		database.addAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						arrayTypeName + "_cmp",
						database.getDefaultNamespace(),
						new String[]{
								"create or replace function " + arrayTypeName + "_cmp(a in " + arrayTypeName +
										", b in " + arrayTypeName + ") return number deterministic is begin " +
										"if a is null or b is null then return null; end if; " +
										"for i in 1 .. least(a.count,b.count) loop " +
										"if a(i) is null or b(i) is null then return null;" +
										"elsif a(i)>b(i) then return 1;" +
										"elsif a(i)<b(i) then return -1; " +
										"end if; " +
										"end loop; " +
										"if a.count=b.count then return 0; elsif a.count>b.count then return 1; else return -1; end if; " +
										"end;"
						},
						new String[] { "drop function " + arrayTypeName + "_cmp" },
						emptySet(),
						false
				)
		);
		database.addAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						arrayTypeName + "_distinct",
						database.getDefaultNamespace(),
						new String[]{
								"create or replace function " + arrayTypeName + "_distinct(a in " + arrayTypeName +
										", b in " + arrayTypeName + ") return number deterministic is begin " +
										"if a is null and b is null then return 0; end if; " +
										"if a is null or b is null or a.count <> b.count then return 1; end if; " +
										"for i in 1 .. a.count loop " +
										"if (a(i) is null)<>(b(i) is null) or a(i)<>b(i) then return 1; end if; " +
										"end loop; " +
										"return 0; " +
										"end;"
						},
						new String[] { "drop function " + arrayTypeName + "_distinct" },
						emptySet(),
						false
				)
		);
		database.addAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						arrayTypeName + "_contains",
						database.getDefaultNamespace(),
						new String[]{
								"create or replace function " + arrayTypeName + "_contains(arr in " + arrayTypeName +
										", elem in " + getRawTypeName( elementType ) + ") return number deterministic is begin " +
										"if arr is null then return null; end if; " +
										"if elem is null then " +
										"for i in 1 .. arr.count loop " +
										"if arr(i) is null then return 1; end if; " +
										"end loop; " +
										"else " +
										"for i in 1 .. arr.count loop " +
										"if arr(i)=elem then return 1; end if; " +
										"end loop; " +
										"end if; " +
										"return 0; " +
										"end;"
						},
						new String[] { "drop function " + arrayTypeName + "_contains" },
						emptySet(),
						false
				)
		);
	}

	private static String getRawTypeName(String typeName) {
		//trim off the length/precision/scale
		final int paren = typeName.indexOf( '(' );
		if ( paren > 0 ) {
			final int parenEnd = typeName.lastIndexOf( ')' );
			return parenEnd + 1 == typeName.length()
					? typeName.substring( 0, paren )
					: typeName.substring( 0, paren ) + typeName.substring( parenEnd + 1 );
		}
		return typeName;
	}

	@Override
	public String getFriendlyName() {
		return typeName;
	}

	@Override
	public String toString() {
		return "OracleArrayTypeDescriptor(" + typeName + ")";
	}
}

