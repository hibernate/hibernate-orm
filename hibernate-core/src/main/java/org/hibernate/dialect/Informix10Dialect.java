package org.hibernate.dialect;

import org.hibernate.dialect.pagination.InformixLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;

/**
 * Since version 10.00.xC3 Informix has limit/offset support which was introduced in July 2005. 
 */
public class Informix10Dialect extends InformixDialect {

	@Override
	public LimitHandler getLimitHandler() {
		return new InformixLimitHandler();
	}
	
}
