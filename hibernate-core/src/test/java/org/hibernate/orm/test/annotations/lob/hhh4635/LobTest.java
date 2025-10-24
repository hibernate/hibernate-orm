/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.lob.hhh4635;

import org.hibernate.dialect.OracleDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.hibernate.Hibernate.getLobHelper;

/**
 * To reproduce this issue, Oracle MUST use a multi-byte character set (UTF-8)!
 *
 * @author Brett Meyer
 */
@RequiresDialect( OracleDialect.class )
@JiraKey( value = "HHH-4635" )
@DomainModel(
		annotatedClasses = {
				LobTestEntity.class
		}
)
@SessionFactory
public class LobTest  {
	static final Logger LOG = Logger.getLogger( LobTest.class.getName() );

	@Test
	public void hibernateTest(SessionFactoryScope scope) {
		printConfig(scope);

		scope.inTransaction(
				session -> {
					LobTestEntity entity = new LobTestEntity();
					entity.setId( 1L );
					entity.setLobValue( getLobHelper().createBlob( new byte[9999] ) );
					entity.setQwerty( randomString( 4000 ) );
					session.persist( entity );
				}
		);
	}

	private String randomString( int count ) {
		StringBuilder buffer = new StringBuilder(count);
		for( int i = 0; i < count; i++ ) {
			buffer.append( 'a' );
		}
		return buffer.toString();
	}

	private void printConfig(SessionFactoryScope scope) {
		String sql = "select value from V$NLS_PARAMETERS where parameter = 'NLS_CHARACTERSET'";

		scope.inTransaction(
				session -> {
					String s = session.createNativeQuery( sql, String.class ).uniqueResult();
					LOG.info( "Using Oracle charset " + s );
				}
		);
	}
}
