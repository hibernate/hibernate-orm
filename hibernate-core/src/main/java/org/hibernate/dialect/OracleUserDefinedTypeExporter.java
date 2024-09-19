/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import java.util.Locale;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedNameParser;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.mapping.UserDefinedArrayType;
import org.hibernate.tool.schema.internal.StandardUserDefinedTypeExporter;
import org.hibernate.type.SqlTypes;

import static java.sql.Types.BOOLEAN;
import static org.hibernate.type.SqlTypes.BIGINT;
import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.DATE;
import static org.hibernate.type.SqlTypes.INTEGER;
import static org.hibernate.type.SqlTypes.LONG32VARBINARY;
import static org.hibernate.type.SqlTypes.SMALLINT;
import static org.hibernate.type.SqlTypes.TABLE;
import static org.hibernate.type.SqlTypes.TIME;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_UTC;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.VARBINARY;

/**
 * @author Christian Beikov
 */
public class OracleUserDefinedTypeExporter extends StandardUserDefinedTypeExporter {

	public OracleUserDefinedTypeExporter(Dialect dialect) {
		super( dialect );
	}

	@Override
	public String[] getSqlCreateStrings(
			UserDefinedArrayType userDefinedType,
			Metadata metadata,
			SqlStringGenerationContext context) {
		final QualifiedName typeName = new QualifiedNameParser.NameParts(
				Identifier.toIdentifier( userDefinedType.getCatalog(), userDefinedType.isCatalogQuoted() ),
				Identifier.toIdentifier( userDefinedType.getSchema(), userDefinedType.isSchemaQuoted() ),
				userDefinedType.getNameIdentifier()
		);

		final String arrayTypeName = context.format( typeName );
		final Integer arraySqlTypeCode = userDefinedType.getArraySqlTypeCode();
		final String elementType = userDefinedType.getElementTypeName();
		if ( arraySqlTypeCode == null || arraySqlTypeCode == TABLE ) {
			return new String[] {
					"create or replace type " + arrayTypeName + " as table of " + elementType
			};
		}
		final int arrayLength = userDefinedType.getArrayLength();
		final Integer elementSqlTypeCode = userDefinedType.getElementSqlTypeCode();
		final String jsonTypeName = metadata.getDatabase().getTypeConfiguration().getDdlTypeRegistry().getTypeName(
				SqlTypes.JSON,
				dialect
		);
		final String valueExpression = determineValueExpression( "t.value", elementSqlTypeCode, elementType );
		return new String[] {
				"create or replace type " + arrayTypeName + " as varying array(" + arrayLength + ") of " + elementType,
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
						"end;",
				"create or replace function " + arrayTypeName + "_distinct(a in " + arrayTypeName +
						", b in " + arrayTypeName + ") return number deterministic is begin " +
						"if a is null and b is null then return 0; end if; " +
						"if a is null or b is null or a.count <> b.count then return 1; end if; " +
						"for i in 1 .. a.count loop " +
						"if (a(i) is null)<>(b(i) is null) or a(i)<>b(i) then return 1; end if; " +
						"end loop; " +
						"return 0; " +
						"end;",
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
						"end;",
				"create or replace function " + arrayTypeName + "_length(arr in " + arrayTypeName +
						") return number deterministic is begin " +
						"if arr is null then return null; end if; " +
						"return arr.count; " +
						"end;",
				createOrReplaceConcatFunction( arrayTypeName ),
				"create or replace function " + arrayTypeName + "_includes(haystack in " + arrayTypeName +
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
						"end;",
				"create or replace function " + arrayTypeName + "_intersects(haystack in " + arrayTypeName +
						", needle in " + arrayTypeName + ", nullable in number) return number deterministic is begin " +
						"if haystack is null or needle is null then return null; end if; " +
						"if needle.count = 0 then return 1; end if; " +
						"for i in 1 .. needle.count loop " +
						"for j in 1 .. haystack.count loop " +
						"if nullable = 1 and needle(i) is null and haystack(j) is null or needle(i)=haystack(j) then return 1; end if; " +
						"end loop; " +
						"end loop; " +
						"return 0; " +
						"end;",
				"create or replace function " + arrayTypeName + "_get(arr in " + arrayTypeName +
						", idx in number) return " + getRawTypeName( elementType ) + " deterministic is begin " +
						"if arr is null or idx is null or arr.count < idx then return null; end if; " +
						"return arr(idx); " +
						"end;",
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
						"end;",
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
						"end;",
				"create or replace function " + arrayTypeName + "_remove_index(arr in " + arrayTypeName +
						", idx in number) return " + arrayTypeName + " deterministic is " +
						"res " + arrayTypeName + ":=" + arrayTypeName + "(); begin " +
						"if arr is null or idx is null then return arr; end if; " +
						"for i in 1 .. arr.count loop " +
						"if i<>idx then res.extend; res(res.last) := arr(i); end if; " +
						"end loop; " +
						"return res; " +
						"end;",
				"create or replace function " + arrayTypeName + "_slice(arr in " + arrayTypeName +
						", startIdx in number, endIdx in number) return " + arrayTypeName + " deterministic is " +
						"res " + arrayTypeName + ":=" + arrayTypeName + "(); begin " +
						"if arr is null or startIdx is null or endIdx is null then return null; end if; " +
						"for i in startIdx .. least(arr.count,endIdx) loop " +
						"res.extend; res(res.last) := arr(i); " +
						"end loop; " +
						"return res; " +
						"end;",
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
						"end;",
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
						"end;",
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
						"end;",
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
						"end;",
				"create or replace function " + arrayTypeName + "_to_string(arr in " + arrayTypeName +
						", sep in varchar2, nullVal in varchar2) return varchar2 deterministic is " +
						"res varchar2(4000):=''; begin " +
						"if arr is null or sep is null then return null; end if; " +
						"for i in 1 .. arr.count loop " +
						"if arr(i) is not null then " +
						"if length(res)<>0 then res:=res||sep; end if; " +
						"res:=res||arr(i); " +
						"elsif nullVal is not null then " +
						"if length(res)<>0 then res:=res||sep; end if; " +
						"res:=res||nullVal; " +
						"end if; " +
						"end loop; " +
						"return res; " +
						"end;",
				"create or replace function " + arrayTypeName + "_from_json(arr in " + jsonTypeName +
						") return " + arrayTypeName + " deterministic is " +
						"res " + arrayTypeName + ":=" + arrayTypeName + "(); begin " +
						"if arr is null then return null; end if; " +
						"select " + valueExpression + " bulk collect into res " +
						"from json_table(arr,'$[*]' columns (value path '$')) t; " +
						"return res; " +
						"end;"
		};
	}

	@Override
	public String[] getSqlDropStrings(UserDefinedArrayType userDefinedType, Metadata metadata, SqlStringGenerationContext context) {
		final QualifiedName typeName = new QualifiedNameParser.NameParts(
				Identifier.toIdentifier( userDefinedType.getCatalog(), userDefinedType.isCatalogQuoted() ),
				Identifier.toIdentifier( userDefinedType.getSchema(), userDefinedType.isSchemaQuoted() ),
				userDefinedType.getNameIdentifier()
		);

		final String arrayTypeName = context.format( typeName );
		final Integer arraySqlTypeCode = userDefinedType.getArraySqlTypeCode();
		if ( arraySqlTypeCode == null || arraySqlTypeCode == TABLE ) {
			return new String[] {
					buildDropTypeSqlString(arrayTypeName)
			};
		}
		return new String[] {
				buildDropTypeSqlString(arrayTypeName),
				buildDropFunctionSqlString(arrayTypeName + "_cmp"),
				buildDropFunctionSqlString(arrayTypeName + "_distinct"),
				buildDropFunctionSqlString(arrayTypeName + "_position"),
				buildDropFunctionSqlString(arrayTypeName + "_length"),
				buildDropFunctionSqlString(arrayTypeName + "_concat"),
				buildDropFunctionSqlString(arrayTypeName + "_includes"),
				buildDropFunctionSqlString(arrayTypeName + "_intersects"),
				buildDropFunctionSqlString(arrayTypeName + "_get"),
				buildDropFunctionSqlString(arrayTypeName + "_set"),
				buildDropFunctionSqlString(arrayTypeName + "_remove"),
				buildDropFunctionSqlString(arrayTypeName + "_remove_index"),
				buildDropFunctionSqlString(arrayTypeName + "_slice"),
				buildDropFunctionSqlString(arrayTypeName + "_replace"),
				buildDropFunctionSqlString(arrayTypeName + "_trim"),
				buildDropFunctionSqlString(arrayTypeName + "_fill"),
				buildDropFunctionSqlString(arrayTypeName + "_positions"),
				buildDropFunctionSqlString(arrayTypeName + "_to_string"),
				buildDropFunctionSqlString(arrayTypeName + "_from_json")
		};
	}

	private String buildDropTypeSqlString(String arrayTypeName) {
		if ( dialect.supportsIfExistsBeforeTypeName() ) {
			return "drop type if exists " + arrayTypeName + " force";
		}
		else {
			return "drop type " + arrayTypeName + " force";
		}
	}

	private String buildDropFunctionSqlString(String functionTypeName) {
		if ( supportsIfExistsBeforeFunctionName() ) {
			return "drop function if exists " + functionTypeName;
		}
		else {
			return "drop function " + functionTypeName;
		}
	}

	private boolean supportsIfExistsBeforeFunctionName() {
		return dialect.getVersion().isSameOrAfter( 23 );
	}

	private String determineValueExpression(String expression, int elementSqlTypeCode, String elementType) {
		switch ( elementSqlTypeCode ) {
			case BOOLEAN:
				if ( elementType.toLowerCase( Locale.ROOT ).trim().startsWith( "number" ) ) {
					return "decode(" + expression + ",'true',1,'false',0,null)";
				}
			case TINYINT:
			case SMALLINT:
			case INTEGER:
			case BIGINT:
				return "cast(" + expression + " as " + elementType + ")";
			case DATE:
				return "to_date(" + expression + ",'YYYY-MM-DD')";
			case TIME:
				return "to_timestamp(" + expression + ",'hh24:mi:ss')";
			case TIMESTAMP:
				return "to_timestamp(" + expression + ",'YYYY-MM-DD\"T\"hh24:mi:ss.FF9')";
			case TIMESTAMP_WITH_TIMEZONE:
			case TIMESTAMP_UTC:
				return "to_timestamp_tz(" + expression + ",'YYYY-MM-DD\"T\"hh24:mi:ss.FF9TZH:TZM')";
			case BINARY:
			case VARBINARY:
			case LONG32VARBINARY:
				return "hextoraw(" + expression + ")";
			default:
				return expression;
		}
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

	protected String getRawTypeName(String typeName) {
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
}
