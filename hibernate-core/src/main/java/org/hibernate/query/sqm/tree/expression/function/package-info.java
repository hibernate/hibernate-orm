/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * = Support for functions in SQM
 *
 * This package defines Hibernate's support for functions in SQM, which includes:
 *
 * 		* Standard functions, which are inherently recognized/handled and
 * 			are available regardless of the underlying Dialect and database.
 * 			These include
 * 			* those defined by JPA
 *				* abs
 *				* avg
 *				* concat
 *				* count
 * 				* current_date
 *		 		* current_time
 * 				* current_timestamp
 * 				* length
 *		 		* locate
 * 				* lower
 * 				* max
 * 				* min
 * 				* mod
 *		 		* sqrt
 *		 		* substring
 * 				* sum
 * 				* trim
 * 				* upper
 * 			* Hibernate extensions
 *		 		* bit_length
 * 				* coalesce		- not technically a function, but supported as such to allow overriding
 * 				* nullif		- not technically a function, but supported as such to allow overriding
 * 				* cast
 * 				* extract
 * 				* second		- generally defined as `extract(second from ?1)`
 * 				* minute		- generally defined as `extract(minute from ?1)`
 * 				* hour			- generally defined as `extract(hour from ?1)`
 * 				* day			- generally defined as `extract(day from ?1)`
 * 				* month			- generally defined as `extract(month from ?1)`
 *		 		* year			- generally defined as `extract(year from ?1)`
 * 				* str 			- generally defined as `cast(?1 as CHAR )`
 * 		* non-standard functions
 * 			* using JPA's `function("function_name, [args]*)` syntax.
 * 			* directly leveraging Hibernate's {@link org.hibernate.query.sqm.produce.function.SqmFunctionRegistry}
 *
 * All standard functions have dedicated node classes for both SQM and SQL AST
 * as sub-types of {@link org.hibernate.query.sqm.tree.expression.function.SqmStandardFunction} and
 * {@link org.hibernate.sql.ast.tree.spi.expression.StandardFunction} respectively.
 * <p/>
 * Support for non-standard functions uses {@link org.hibernate.query.sqm.tree.expression.function.SqmGenericFunction}
 * and {@link org.hibernate.sql.ast.tree.spi.expression.NonStandardFunction} instead.
 */
package org.hibernate.query.sqm.tree.expression.function;
