package org.hibernate.tool.api.reveng;


import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.exception.spi.SQLExceptionConverter;
import org.hibernate.mapping.Table;

/**
 * Provides runtime-only information for reverse engineering process.
 * e.g. current connection provider, exception converter etc. 
 * 
 * @author max
 * @author koen
 *
 */
public class ReverseEngineeringRuntimeInfo {

	private final ConnectionProvider connectionProvider;
	private final SQLExceptionConverter SQLExceptionConverter;
	private final DatabaseCollector dbs;
	
	public static ReverseEngineeringRuntimeInfo createInstance(ConnectionProvider provider, SQLExceptionConverter sec, DatabaseCollector dbs) {
		return new ReverseEngineeringRuntimeInfo(provider,sec,dbs);
	}
	
	private ReverseEngineeringRuntimeInfo(ConnectionProvider provider, SQLExceptionConverter sec, DatabaseCollector dbs) {
		this.connectionProvider = provider;
		this.SQLExceptionConverter = sec;
		this.dbs = dbs;
	}
	
	public ConnectionProvider getConnectionProvider() {
		return connectionProvider;
	}
	
	public SQLExceptionConverter getSQLExceptionConverter() {
		return SQLExceptionConverter;
	}
	
	public Table getTable(TableIdentifier ti) {
		return dbs.getTable(ti.getSchema(), ti.getCatalog(), ti.getName());
	}	
		
}
