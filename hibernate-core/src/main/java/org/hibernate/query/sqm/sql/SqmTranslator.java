/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql;

import org.hibernate.query.sqm.tree.cte.SqmCteStatement;
import org.hibernate.sql.ast.tree.cte.CteStatement;

/**
 * @author Steve Ebersole
 */
public interface SqmTranslator {
	CteStatement translate(SqmCteStatement sqmCte);
}
