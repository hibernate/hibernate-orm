package org.hibernate.orm.test.annotations.namingstrategy;

import org.hibernate.boot.model.naming.ColumnNamingContext;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

public class ContextNamingStrategy extends PhysicalNamingStrategyStandardImpl {
	@Override
	public Identifier toPhysicalColumnName(
			Identifier logicalName,
			JdbcEnvironment jdbcEnvironment,
			ColumnNamingContext columnNamingContext) {
		final String entityName = columnNamingContext.entityName();
		return entityName == null
				? logicalName
				: new Identifier( simpleName( entityName ) + '_' + logicalName.getText(), logicalName.isQuoted() );
	}

	@Override
	public Identifier toPhysicalColumnName(Identifier logicalName, JdbcEnvironment context) {
		return new Identifier( "legacy_" + logicalName.getText(), logicalName.isQuoted() );
	}

	private String simpleName(String typeName) {
		final int separator = Math.max( typeName.lastIndexOf( '.' ), typeName.lastIndexOf( '$' ) );
		return separator < 0 ? typeName : typeName.substring( separator + 1 );
	}
}
