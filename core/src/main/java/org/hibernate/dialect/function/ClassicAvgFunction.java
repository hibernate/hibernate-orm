/**
 * 
 */
package org.hibernate.dialect.function;

import java.sql.Types;

import org.hibernate.Hibernate;
import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.engine.Mapping;
import org.hibernate.type.Type;

/**
 * Classic AVG sqlfunction that return types as it was done in Hibernate 3.1 
 * 
 * @author Max Rydahl Andersen
 *
 */
public class ClassicAvgFunction extends StandardSQLFunction {
	public ClassicAvgFunction() {
		super( "avg" );
	}

	public Type getReturnType(Type columnType, Mapping mapping) throws QueryException {
		int[] sqlTypes;
		try {
			sqlTypes = columnType.sqlTypes( mapping );
		}
		catch ( MappingException me ) {
			throw new QueryException( me );
		}
		if ( sqlTypes.length != 1 ) throw new QueryException( "multi-column type in avg()" );
		int sqlType = sqlTypes[0];
		if ( sqlType == Types.INTEGER || sqlType == Types.BIGINT || sqlType == Types.TINYINT ) {
			return Hibernate.FLOAT;
		}
		else {
			return columnType;
		}
	}
}