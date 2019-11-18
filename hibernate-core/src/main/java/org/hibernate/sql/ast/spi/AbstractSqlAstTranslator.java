/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.spi;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlTreeCreationException;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.update.Assignment;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqlAstTranslator
		extends AbstractSqlAstWalker
		implements SqlAstTranslator {

	private final Set<String> affectedTableNames = new HashSet<>();

	protected AbstractSqlAstTranslator(SessionFactoryImplementor sessionFactory) {
		super( sessionFactory );
	}

	@Override
	public void visitAssignment(Assignment assignment) {
		throw new SqlTreeCreationException ( "Encountered unexpected assignment clause" );
	}

	@Override
	public Set<String> getAffectedTableNames() {
		return affectedTableNames;
	}

	@Override
	protected void renderTableReference(TableReference tableReference) {
		super.renderTableReference( tableReference );
		registerAffectedTable( tableReference );
	}

	protected void registerAffectedTable(TableReference tableReference) {
		registerAffectedTable( tableReference.getTableExpression() );
	}

	protected void registerAffectedTable(String tableExpression) {
		affectedTableNames.add( tableExpression );
	}
}
