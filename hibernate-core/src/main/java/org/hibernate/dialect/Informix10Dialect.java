/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.dialect.pagination.Informix10LimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;

/**
 * Since version 10.00.xC3 Informix has limit/offset support which was introduced in July 2005. 
 */
public class Informix10Dialect extends InformixDialect {

	@Override
	public LimitHandler getLimitHandler() {
		return Informix10LimitHandler.INSTANCE;
	}
	
}
