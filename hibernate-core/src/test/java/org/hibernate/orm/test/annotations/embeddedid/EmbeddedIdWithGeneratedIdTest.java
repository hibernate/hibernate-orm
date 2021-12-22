/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.embeddedid;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Emmanuel Bernard
 */
@DomainModel(
		annotatedClasses = Dvd.class
)
@SessionFactory
public class EmbeddedIdWithGeneratedIdTest {

	@Test
	public void testIdWithMulticolumns(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Dvd lesOiseaux = new Dvd();
					lesOiseaux.setTitle( "Les oiseaux" );
					session.persist( lesOiseaux );
					session.flush();
					assertNotNull( lesOiseaux.getId() );
				}
		);
	}

}
