/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast;

import java.util.Set;

import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptorIndicators;

/**
 * @author Steve Ebersole
 */
public interface SqlAstTranslator extends SqlAstWalker, SqlTypeDescriptorIndicators {
	/**
	 * Not the best spot for this.  Its the table names collected while walking the SQL AST.
	 * Its ok here because the translator is consider a one-time-use.  It just needs to be called
	 * after translation.
	 *
	 * A better option is probably to have "translation" objects that expose the affected table-names.
	 */
	Set<String> getAffectedTableNames();

	/**
	 * Generalized support for translating a CTE statement.  The underlying
	 * {@link CteStatement#getCteConsumer()} could be a SELECT, UPDATE, DELETE, etc.
	 *
	 * Implementors may throw an exception is the CTE-consumer is of the incorrect type
	 */
	JdbcOperation translate(CteStatement cteStatement);
}
