/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.util;

import org.hibernate.hql.internal.antlr.HqlTokenTypes;
import org.hibernate.hql.internal.antlr.SqlTokenTypes;
import org.hibernate.sql.ordering.antlr.GeneratedOrderByFragmentRendererTokenTypes;

/**
 * Commonly used token printers expressed as constants.
 */
public interface TokenPrinters {

	ASTPrinter HQL_TOKEN_PRINTER = new ASTPrinter( HqlTokenTypes.class );

	ASTPrinter SQL_TOKEN_PRINTER = new ASTPrinter( SqlTokenTypes.class );

	ASTPrinter ORDERBY_FRAGMENT_PRINTER = new ASTPrinter( GeneratedOrderByFragmentRendererTokenTypes.class );

	ASTPrinter REFERENCED_TABLES_PRINTER = new ASTReferencedTablesPrinter( SqlTokenTypes.class );

}
