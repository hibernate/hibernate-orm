/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.lob;

import java.util.Random;

import org.junit.Test;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Chris Cranford
 */
@RequiresDialectFeature(DialectChecks.ForceLobAsLastValue.class)
@TestForIssue(jiraKey = "HHH-8382")
public class LobAsLastValueTest extends BaseCoreFunctionalTestCase {
	@Override
	protected String[] getMappings() {
		return new String[] { "lob/LobAsLastValue.hbm.xml" };
	}

	@Test
	public void testInsertLobAsLastValue() {
		doInHibernate( this::sessionFactory, session -> {
			byte[] details = new byte[4000];
			byte[] title = new byte[2000];

			Random random = new Random();
			random.nextBytes( details );
			random.nextBytes( title );

			// This insert will fail on Oracle without the fix to ModelBinder flagging SimpleValue and Property as Lob
			// because the fields will not be placed at the end of the insert, resulting in an Oracle failure.
			final LobAsLastValueEntity entity = new LobAsLastValueEntity( "Test", new String( details ), new String( title ) );
			session.save( entity );
		} );
	}

}
