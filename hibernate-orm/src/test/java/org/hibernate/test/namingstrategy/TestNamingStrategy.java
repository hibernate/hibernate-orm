/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.namingstrategy;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitBasicColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

/**
 * @author Emmanuel Bernard
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class TestNamingStrategy extends ImplicitNamingStrategyJpaCompliantImpl implements PhysicalNamingStrategy {
	/**
	 * Singleton access
	 */
	public static final TestNamingStrategy INSTANCE = new TestNamingStrategy();

	public TestNamingStrategy() {
	}

	@Override
	public Identifier determineBasicColumnName(ImplicitBasicColumnNameSource source) {
		return toIdentifier(
				"PTCN_" + source.getAttributePath().getProperty(),
				source.getBuildingContext()
		);
	}

	@Override
	public Identifier toPhysicalCatalogName(Identifier name, JdbcEnvironment context) {
		return PhysicalNamingStrategyStandardImpl.INSTANCE.toPhysicalCatalogName( name, context );
	}

	@Override
	public Identifier toPhysicalSchemaName(Identifier name, JdbcEnvironment jdbcEnvironment) {
		return PhysicalNamingStrategyStandardImpl.INSTANCE.toPhysicalSchemaName( name, jdbcEnvironment );
	}

	@Override
	public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment jdbcEnvironment) {
		return Identifier.toIdentifier( "TAB_" + name.getText() );
	}

	@Override
	public Identifier toPhysicalSequenceName(Identifier name, JdbcEnvironment jdbcEnvironment) {
		return PhysicalNamingStrategyStandardImpl.INSTANCE.toPhysicalSequenceName( name, jdbcEnvironment );
	}

	@Override
	public Identifier toPhysicalColumnName(Identifier name, JdbcEnvironment jdbcEnvironment) {
		if ( name.getText().startsWith( "PTCN_" ) ) {
			return name;
		}
		else {
			return Identifier.toIdentifier( "CN_" + name.getText() );
		}
	}
}
