package org.hibernate.orm.test.jpa;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

/**
 * @author Emmanuel Bernard
 */
public class MyNamingStrategy extends PhysicalNamingStrategyStandardImpl {
	@Override
	public Identifier toPhysicalTableName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
		return jdbcEnvironment.getIdentifierHelper().toIdentifier( "tbl_" + logicalName.getText() );
	}
}
