/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.generatedkeys.select;

import org.hibernate.dialect.OracleDialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/generatedkeys/select/MyEntity.hbm.xml"
)
@SessionFactory
@RequiresDialect(value = OracleDialect.class, version = 900)
public class SelectGeneratorTest {

	@Test
	public void testJDBC3GetGeneratedKeysSupportOnOracle(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					MyEntity e = new MyEntity( "entity-1" );
					session.save( e );

					// this insert should happen immediately!
					assertEquals( new Long(1), e.getId(), "id not generated through forced insertion" );

					session.delete( e );
				}
		);
	}
}
