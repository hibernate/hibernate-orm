/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql;

import org.hibernate.query.sqm.spi.JdbcParameterBySqmParameterAccess;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;

/**
 * @author Steve Ebersole
 */
public interface SimpleSqmUpdateTranslator extends SqmTranslator, SqmToSqlAstConverter, JdbcParameterBySqmParameterAccess {
	SimpleSqmUpdateTranslation translate(SqmUpdateStatement sqmUpdate);
}
