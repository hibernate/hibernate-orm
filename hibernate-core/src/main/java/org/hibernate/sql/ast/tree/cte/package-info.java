/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

// Support for SQL CTE statements (WITH).  The general syntax for a CTE statement is:
//
// 	cte:: WITH {cte-label} OPEN_PAREN cteColumns CLOSE_PAREN AS cteDefinition consumer
//
// 	cteColumns:: cteColumn (, cteColumn)
//
// 	todo (6.0) : should this include not-null, etc?
// 	cteColumn:: ...
//
// 	cteDefinition:: querySpec
//
// 	todo (6.0) : imo it would be better to have a specific contract `CteConsumer` for things that can occur here, which are:
// 			* select - `QuerySpec`
// 			* delete - `DeleteStatement`
// 			* update - `UpdateStatement`
// 			* insert-select - `InsertStatement
//
// 	consumer:: querySpec | deleteStatement | updateStatement | insertSelectStatement
//
// 	for example, a delete consumer might look like:
//
// 			with cte_name ( col1, col2, col3 ) as (
// 				select some_val1, some_val, some_v3
// 				from some_place
// 			)
// 			delete from some_table
// 			where (some_table_col1, some_table_col2, some_table_col3) in (
// 				select col1, col2, col3
// 				from cte_name
// 			)
/**
 * Support for common table expressions (CTE) in a SQL tree.
 */
package org.hibernate.sql.ast.tree.cte;
