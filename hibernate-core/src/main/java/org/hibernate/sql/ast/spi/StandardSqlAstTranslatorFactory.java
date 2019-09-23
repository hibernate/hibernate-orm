/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.SqlAstSelectTranslator;

/**
 * @author Steve Ebersole
 */
public class StandardSqlAstTranslatorFactory implements SqlAstTranslatorFactory {
	@Override
	public SqlAstSelectTranslator buildSelectConverter(SessionFactoryImplementor sessionFactory) {
		return new StandardSqlAstSelectTranslator( sessionFactory);
	}
}
