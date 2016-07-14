/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.ast.expression;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.sqm.exec.results.spi.ReturnReader;
import org.hibernate.sql.sqm.convert.spi.SqlTreeWalker;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public interface Expression {
	/**
	 * Obtain the ORM Type of this expression
	 *
	 * @return The ORM mapping Type
	 */
	Type getType();

	/**
	 * Obtain the ReturnReader for properly reading back values for this expression from the select
	 *
	 * @return The Return
	 */
	ReturnReader getReturnReader(int startPosition, boolean shallow, SessionFactoryImplementor sessionFactory);

	/**
	 * Visitation by delegation
	 *
	 * @param sqlTreeWalker The visitation controller
	 */
	void accept(SqlTreeWalker sqlTreeWalker);
}
