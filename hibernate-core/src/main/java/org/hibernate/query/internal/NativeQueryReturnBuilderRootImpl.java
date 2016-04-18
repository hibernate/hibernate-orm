package org.hibernate.query.internal;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.LockMode;
import org.hibernate.SQLQuery;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryRootReturn;

/**
 * @author Steve Ebersole
 */
public class NativeQueryReturnBuilderRootImpl implements SQLQuery.RootReturn, NativeQueryReturnBuilder {
	private final String alias;
	private final String entityName;
	private LockMode lockMode = LockMode.READ;
	private Map<String, String[]> propertyMappings;

	public NativeQueryReturnBuilderRootImpl(String alias, String entityName) {
		this.alias = alias;
		this.entityName = entityName;
	}

	public SQLQuery.RootReturn setLockMode(LockMode lockMode) {
		this.lockMode = lockMode;
		return this;
	}

	public SQLQuery.RootReturn setDiscriminatorAlias(String alias) {
		addProperty( "class", alias );
		return this;
	}

	public SQLQuery.RootReturn addProperty(String propertyName, String columnAlias) {
		addProperty( propertyName ).addColumnAlias( columnAlias );
		return this;
	}

	public SQLQuery.ReturnProperty addProperty(final String propertyName) {
		if ( propertyMappings == null ) {
			propertyMappings = new HashMap<String, String[]>();
		}
		return new SQLQuery.ReturnProperty() {
			public SQLQuery.ReturnProperty addColumnAlias(String columnAlias) {
				String[] columnAliases = propertyMappings.get( propertyName );
				if ( columnAliases == null ) {
					columnAliases = new String[] {columnAlias};
				}
				else {
					String[] newColumnAliases = new String[columnAliases.length + 1];
					System.arraycopy( columnAliases, 0, newColumnAliases, 0, columnAliases.length );
					newColumnAliases[columnAliases.length] = columnAlias;
					columnAliases = newColumnAliases;
				}
				propertyMappings.put( propertyName, columnAliases );
				return this;
			}
		};
	}

	public NativeSQLQueryReturn buildReturn() {
		return new NativeSQLQueryRootReturn( alias, entityName, propertyMappings, lockMode );
	}
}
