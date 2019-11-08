/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast;

import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * @author Steve Ebersole
 */
public interface SqlAstTranslatorFactory {
	SqlAstSelectTranslator buildSelectConverter(SessionFactoryImplementor sessionFactory);
	SqlAstDeleteTranslator buildDeleteConverter(SessionFactoryImplementor sessionFactory);

	// todo (6.0) : update, delete, etc
}
