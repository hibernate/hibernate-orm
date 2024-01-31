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
						arrayTypeName + "_position",
						database.getDefaultNamespace(),
						new String[]{
								"create or replace function " + arrayTypeName + "_position(arr in " + arrayTypeName +
										", elem in " + getRawTypeName( elementType ) + ", startPos in number default 1) return number deterministic is begin " +
										"if arr is null then return null; end if; " +
										"if elem is null then " +
										"for i in startPos .. arr.count loop " +
										"if arr(i) is null then return i; end if; " +
										"end loop; " +
										"else " +
										"for i in startPos .. arr.count loop " +
										"if arr(i)=elem then return i; end if; " +
										"end loop; " +
										"end if; " +
										"return 0; " +
										"end;"
						},
						new String[] { "drop function " + arrayTypeName + "_position" },
						emptySet(),
						false
				)
		);
		database.addAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						arrayTypeName + "_length",
						database.getDefaultNamespace(),
						new String[]{
								"create or replace function " + arrayTypeName + "_length(arr in " + arrayTypeName +
										") return number deterministic is begin " +
										"if arr is null then return null; end if; " +
										"return arr.count; " +
										"end;"
						},
						new String[] { "drop function " + arrayTypeName + "_length" },
						emptySet(),
						false
				)
		);
		database.addAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						arrayTypeName + "_concat",
						database.getDefaultNamespace(),
						new String[]{ createOrReplaceConcatFunction( arrayTypeName ) },
						new String[] { "drop function " + arrayTypeName + "_concat" },
						emptySet(),
						false
				)
		);
		database.addAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						arrayTypeName + "_contains",
						database.getDefaultNamespace(),
						new String[]{
								"create or replace function " + arrayTypeName + "_contains(haystack in " + arrayTypeName +
										", needle in " + arrayTypeName + ", nullable in number) return number deterministic is found number(1,0); begin " +
										"if haystack is null or needle is null then return null; end if; " +
										"for i in 1 .. needle.count loop " +
										"found := 0; " +
										"for j in 1 .. haystack.count loop " +
										"if nullable = 1 and needle(i) is null and haystack(j) is null or needle(i)=haystack(j) then found := 1; exit; end if; " +
										"end loop; " +
										"if found = 0 then return 0; end if;" +
										"end loop; " +
										"return 1; " +
										"end;"
						},
						new String[] { "drop function " + arrayTypeName + "_contains" },
						emptySet(),
						false
				)
		);
		database.addAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						arrayTypeName + "_overlaps",
						database.getDefaultNamespace(),
						new String[]{
								"create or replace function " + arrayTypeName + "_overlaps(haystack in " + arrayTypeName +
										", needle in " + arrayTypeName + ", nullable in number) return number deterministic is begin " +
										"if haystack is null or needle is null then return null; end if; " +
										"if needle.count = 0 then return 1; end if; " +
										"for i in 1 .. needle.count loop " +
										"for j in 1 .. haystack.count loop " +
										"if nullable = 1 and needle(i) is null and haystack(j) is null or needle(i)=haystack(j) then return 1; end if; " +
										"end loop; " +
										"end loop; " +
										"return 0; " +
										"end;"
						},
						new String[] { "drop function " + arrayTypeName + "_overlaps" },
						emptySet(),
						false
				)
		);
		database.addAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						arrayTypeName + "_get",
						database.getDefaultNamespace(),
						new String[]{
								"create or replace function " + arrayTypeName + "_get(arr in " + arrayTypeName +
										", idx in number) return " + getRawTypeName( elementType ) + " deterministic is begin " +
										"if arr is null or idx is null or arr.count < idx then return null; end if; " +
										"return arr(idx); " +
										"end;"
						},
						new String[] { "drop function " + arrayTypeName + "_get" },
						emptySet(),
						false
				)
		);
		database.addAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						arrayTypeName + "_set",
						database.getDefaultNamespace(),
						new String[]{
								"create or replace function " + arrayTypeName + "_set(arr in " + arrayTypeName +
										", idx in number, elem in " + getRawTypeName( elementType ) + ") return " + arrayTypeName + " deterministic is " +
										"res " + arrayTypeName + ":=" + arrayTypeName + "(); begin " +
										"if arr is not null then " +
										"for i in 1 .. arr.count loop " +
										"res.extend; " +
										"res(i) := arr(i); " +
										"end loop; " +
										"for i in arr.count+1 .. idx loop " +
										"res.extend; " +
										"end loop; " +
										"else " +
										"for i in 1 .. idx loop " +
										"res.extend; " +
										"end loop; " +
										"end if; " +
										"res(idx) := elem; " +
										"return res; " +
										"end;"
						},
						new String[] { "drop function " + arrayTypeName + "_set" },
						emptySet(),
						false
				)
		);
		database.addAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						arrayTypeName + "_remove",
						database.getDefaultNamespace(),
						new String[]{
								"create or replace function " + arrayTypeName + "_remove(arr in " + arrayTypeName +
										", elem in " + getRawTypeName( elementType ) + ") return " + arrayTypeName + " deterministic is " +
										"res " + arrayTypeName + ":=" + arrayTypeName + "(); begin " +
										"if arr is null then return null; end if; " +
										"if elem is null then " +
										"for i in 1 .. arr.count loop " +
										"if arr(i) is not null then res.extend; res(res.last) := arr(i); end if; " +
										"end loop; " +
										"else " +
										"for i in 1 .. arr.count loop " +
										"if arr(i) is null or arr(i)<>elem then res.extend; res(res.last) := arr(i); end if; " +
										"end loop; " +
										"end if; " +
										"return res; " +
										"end;"
						},
						new String[] { "drop function " + arrayTypeName + "_remove" },
						emptySet(),
						false
				)
		);
		database.addAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						arrayTypeName + "_remove_index",
						database.getDefaultNamespace(),
						new String[]{
								"create or replace function " + arrayTypeName + "_remove_index(arr in " + arrayTypeName +
										", idx in number) return " + arrayTypeName + " deterministic is " +
										"res " + arrayTypeName + ":=" + arrayTypeName + "(); begin " +
										"if arr is null or idx is null then return arr; end if; " +
										"for i in 1 .. arr.count loop " +
										"if i<>idx then res.extend; res(res.last) := arr(i); end if; " +
										"end loop; " +
										"return res; " +
										"end;"
						},
						new String[] { "drop function " + arrayTypeName + "_remove_index" },
						emptySet(),
						false
				)
		);
		database.addAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						arrayTypeName + "_slice",
						database.getDefaultNamespace(),
						new String[]{
								"create or replace function " + arrayTypeName + "_slice(arr in " + arrayTypeName +
										", startIdx in number, endIdx in number) return " + arrayTypeName + " deterministic is " +
										"res " + arrayTypeName + ":=" + arrayTypeName + "(); begin " +
										"if arr is null or startIdx is null or endIdx is null then return null; end if; " +
										"for i in startIdx .. least(arr.count,endIdx) loop " +
										"res.extend; res(res.last) := arr(i); " +
										"end loop; " +
										"return res; " +
										"end;"
						},
						new String[] { "drop function " + arrayTypeName + "_slice" },
						emptySet(),
						false
				)
		);
		database.addAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						arrayTypeName + "_replace",
						database.getDefaultNamespace(),
						new String[]{
								"create or replace function " + arrayTypeName + "_replace(arr in " + arrayTypeName +
										", old in " + getRawTypeName( elementType ) + ", elem in " + getRawTypeName( elementType ) + ") return " + arrayTypeName + " deterministic is " +
										"res " + arrayTypeName + ":=" + arrayTypeName + "(); begin " +
										"if arr is null then return null; end if; " +
										"if old is null then " +
										"for i in 1 .. arr.count loop " +
										"res.extend; " +
										"res(res.last) := coalesce(arr(i),elem); " +
										"end loop; " +
										"else " +
										"for i in 1 .. arr.count loop " +
										"res.extend; " +
										"if arr(i) = old then " +
										"res(res.last) := elem; " +
										"else " +
										"res(res.last) := arr(i); " +
										"end if; " +
										"end loop; " +
										"end if; " +
										"return res; " +
										"end;"
						},
						new String[] { "drop function " + arrayTypeName + "_replace" },
						emptySet(),
						false
				)
		);
		database.addAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						arrayTypeName + "_trim",
						database.getDefaultNamespace(),
						new String[]{
								"create or replace function " + arrayTypeName + "_trim(arr in " + arrayTypeName +
										", elems number) return " + arrayTypeName + " deterministic is " +
										"res " + arrayTypeName + ":=" + arrayTypeName + "(); begin " +
										"if arr is null or elems is null then return null; end if; " +
										"if arr.count < elems then raise_application_error (-20000, 'number of elements to trim must be between 0 and '||arr.count); end if;" +
										"for i in 1 .. arr.count-elems loop " +
										"res.extend; " +
										"res(i) := arr(i); " +
										"end loop; " +
										"return res; " +
										"end;"
						},
						new String[] { "drop function " + arrayTypeName + "_trim" },
						emptySet(),
						false
				)
		);
		database.addAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						arrayTypeName + "_fill",
						database.getDefaultNamespace(),
						new String[]{
								"create or replace function " + arrayTypeName + "_fill(elem in " + getRawTypeName( elementType ) +
										", elems number) return " + arrayTypeName + " deterministic is " +
										"res " + arrayTypeName + ":=" + arrayTypeName + "(); begin " +
										"if elems is null then return null; end if; " +
										"if elems<0 then raise_application_error (-20000, 'number of elements must be greater than or equal to 0'); end if;" +
										"for i in 1 .. elems loop " +
										"res.extend; " +
										"res(i) := elem; " +
										"end loop; " +
										"return res; " +
										"end;"
						},
						new String[] { "drop function " + arrayTypeName + "_fill" },
						emptySet(),
						false
				)
		);
		database.addAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						arrayTypeName + "_positions",
						database.getDefaultNamespace(),
						new String[]{
								"create or replace function " + arrayTypeName + "_positions(arr in " + arrayTypeName +
										", elem in " + getRawTypeName( elementType ) + ") return sdo_ordinate_array deterministic is " +
										"res sdo_ordinate_array:=sdo_ordinate_array(); begin " +
										"if arr is null then return null; end if; " +
										"if elem is null then " +
										"for i in 1 .. arr.count loop " +
										"if arr(i) is null then res.extend; res(res.last):=i; end if; " +
										"end loop; " +
										"else " +
										"for i in 1 .. arr.count loop " +
										"if arr(i)=elem then res.extend; res(res.last):=i; end if; " +
										"end loop; " +
										"end if; " +
										"return res; " +
										"end;"
						},
						new String[] { "drop function " + arrayTypeName + "_positions" },
						emptySet(),
						false
				)
		);
		database.addAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						arrayTypeName + "_to_string",
						database.getDefaultNamespace(),
						new String[]{
								"create or replace function " + arrayTypeName + "_to_string(arr in " + arrayTypeName +
										", sep in varchar2) return varchar2 deterministic is " +
										"res varchar2(4000):=''; begin " +
										"if arr is null or sep is null then return null; end if; " +
										"for i in 1 .. arr.count loop " +
										"if arr(i) is not null then " +
										"if length(res)<>0 then res:=res||sep; end if; " +
										"res:=res||arr(i); " +
										"end if; " +
										"end loop; " +
										"return res; " +
										"end;"
						},
						new String[] { "drop function " + arrayTypeName + "_to_string" },
						emptySet(),
						false
				)
		);
	}

	protected String createOrReplaceConcatFunction(String arrayTypeName) {
		// Since Oracle has no builtin concat function for varrays and doesn't support varargs,
		// we have to create a function with a fixed amount of arguments with default that fits "most" cases.
		// Let's just use 5 for the time being until someone requests more.
		return createOrReplaceConcatFunction( arrayTypeName, 5 );
	}

	protected String createOrReplaceConcatFunction(String arrayTypeName, int maxConcatParams) {
		final StringBuilder sb = new StringBuilder();
		sb.append( "create or replace function " ).append( arrayTypeName ).append( "_concat(" );
		sb.append( "arr0 in " ).append( arrayTypeName ).append( ",arr1 in " ).append( arrayTypeName );
		for ( int i = 2; i < maxConcatParams; i++ ) {
			sb.append( ",arr" ).append( i ).append( " in " ).append( arrayTypeName )
					.append( " default " ).append( arrayTypeName ).append( "()" );
		}
		sb.append( ") return " ).append( arrayTypeName ).append( " deterministic is res " ).append( arrayTypeName )
				.append( "; begin if " );
		String separator = "";
		for ( int i = 0; i < maxConcatParams; i++ ) {
			sb.append( separator ).append( "arr" ).append( i ).append( " is null" );
			separator = " or ";
		}
		sb.append( " then return null; end if; " );
		sb.append( "select * bulk collect into res from (" );
		separator = "";
		for ( int i = 0; i < maxConcatParams; i++ ) {
			sb.append( separator ).append( "select * from table(arr" ).append( i ).append( ')' );
			separator = " union all ";
		}
		return sb.append( "); return res; end;" ).toString();
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

