/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.SqlAstDeleteTranslator;
import org.hibernate.sql.ast.SqlAstInsertSelectTranslator;
import org.hibernate.sql.ast.SqlAstSelectTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.SqlAstUpdateTranslator;

/**
 * @author Steve Ebersole
 */
public class StandardSqlAstTranslatorFactory implements SqlAstTranslatorFactory {
	@Override
	public SqlAstSelectTranslator buildSelectTranslator(SessionFactoryImplementor sessionFactory) {
		return new StandardSqlAstSelectTranslator( sessionFactory);
	}

	@Override
	public SqlAstDeleteTranslator buildDeleteTranslator(SessionFactoryImplementor sessionFactory) {
		return new StandardSqlAstDeleteTranslator( sessionFactory );
	}

	@Override
	public SqlAstInsertSelectTranslator buildInsertTranslator(SessionFactoryImplementor sessionFactory) {
		return new StandardSqlAstInsertSelectTranslator( sessionFactory );
	}

	@Override
	public SqlAstUpdateTranslator buildUpdateTranslator(SessionFactoryImplementor sessionFactory) {
		return new StandardSqlAstUpdateTranslator( sessionFactory );
	}
}
