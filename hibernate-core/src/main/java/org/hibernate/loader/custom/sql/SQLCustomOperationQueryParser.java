/**
 * 
 */
package org.hibernate.loader.custom.sql;

import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * @author Laabidi RAISSI
 *
 */
public class SQLCustomOperationQueryParser extends SQLQueryParser{

	public SQLCustomOperationQueryParser(String queryString, SessionFactoryImplementor factory) {
		super(queryString, null, factory);
	}
	
	@Override
	public String process() {
		return substituteBrackets( originalQueryString );
	}

}
