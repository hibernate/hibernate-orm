/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function;

/**
 * A class that provides name constants for functions that are considered standard in HQL,
 * and are generally implemented by every dialect.
 *
 * This class does not contain constructs such as `size`, `index` etc. which are special functions in JPQL/HQL,
 * that don't translate to real overridable functions.
 */
public final class StandardFunctions {

	public static final String ABS = "abs";
	public static final String ACOS = "acos";
	public static final String ACOSH = "acosh";
	public static final String ACOT = "acot";
	public static final String ACOTH = "acoth";
	public static final String ANY = "any";
	public static final String ASCII = "ascii";
	public static final String ASIN = "asin";
	public static final String ASINH = "asinh";
	public static final String ATAN = "atan";
	public static final String ATAN2 = "atan2";
	public static final String ATANH = "atanh";
	public static final String AVG = "avg";
	public static final String BITAND = "bitand";
	public static final String BITNOT = "bitnot";
	public static final String BITOR = "bitor";
	public static final String BITXOR = "bitxor";
	public static final String BIT_LENGTH = "bit_length";
	public static final String CAST = "cast";
	public static final String CEILING = "ceiling";
	public static final String CHAR = "char";
	public static final String CHARACTER_LENGTH = "character_length";
	public static final String CHR = "chr";
	public static final String COALESCE = "coalesce";
	public static final String COLLATE = "collate";
	public static final String CONCAT = "concat";
	public static final String COS = "cos";
	public static final String COSH = "cosh";
	public static final String COT = "cot";
	public static final String COTH = "coth";
	public static final String COUNT = "count";
	public static final String CURRENT_DATE = "current_date";
	public static final String CURRENT_TIME = "current_time";
	public static final String CURRENT_TIMESTAMP = "current_timestamp";
	public static final String DATEADD = "dateadd";//todo:document
	public static final String DATEDIFF = "datediff";//todo:document
	public static final String DEGREES = "degrees";
	public static final String EVERY = "every";
	public static final String EXP = "exp";
	public static final String EXTRACT = "extract";
	public static final String FIRST_VALUE = "first_value";
	public static final String FLOOR = "floor";
	public static final String FORMAT = "format";
	public static final String GREATEST = "greatest";
	public static final String IFNULL = "ifnull";
	public static final String INSTANT = "instant";
	public static final String LAG = "lag";
	public static final String LAST_VALUE = "last_value";
	public static final String LEAD = "lead";
	public static final String LEAST = "least";
	public static final String LEFT = "left";
	public static final String LENGTH = "length";
	public static final String LISTAGG = "listagg";
	public static final String LN = "ln";
	public static final String LOCAL_DATE = "local_date";
	public static final String LOCAL_DATETIME = "local_datetime";
	public static final String LOCAL_TIME = "local_time";
	public static final String LOCATE = "locate";
	public static final String LOG = "log";
	public static final String LOG10 = "log10";
	public static final String LOG2 = "log2";
	public static final String LOWER = "lower";
	public static final String LPAD = "lpad";//todo:document
	public static final String MAX = "max";
	public static final String MIN = "min";
	public static final String MOD = "mod";
	public static final String NTH_VALUE = "nth_value";
	public static final String NULLIF = "nullif";
	public static final String OCTET_LENGTH = "octet_length";
	public static final String OFFSET_DATETIME = "offset_datetime";
	public static final String OVERLAY = "overlay";
	public static final String PAD = "pad";
	public static final String PI = "pi";
	public static final String POSITION = "position";
	public static final String POWER = "power";
	public static final String RADIANS = "radians";
	public static final String RAND = "rand";
	public static final String REPEAT = "repeat";//todo:document
	public static final String REPLACE = "replace";
	public static final String RIGHT = "right";
	public static final String ROUND = "round";
	public static final String ROW_NUMBER = "row_number";
	public static final String RPAD = "rpad";//todo:document
	public static final String SIGN = "sign";
	public static final String SIN = "sin";
	public static final String SINH = "sinh";
	public static final String SQL = "sql";
	public static final String SQRT = "sqrt";
	public static final String STR = "str";
	public static final String SUBSTRING = "substring";
	public static final String SUM = "sum";
	public static final String TAN = "tan";
	public static final String TANH = "tanh";
	public static final String TIMESTAMPADD = "timestampadd";//todo:document
	public static final String TIMESTAMPDIFF = "timestampdiff";//todo:document
	public static final String TRIM = "trim";
	public static final String UPPER = "upper";

	/**
	 * Disallow instantiation
	 */
	private StandardFunctions() {
	}
}
