/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.cascade;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;


@DomainModel(
		annotatedClasses = {
				CodedPairSetHolder.class,
				CodedPairHolder.class,
				Person.class,
				PersonPair.class
		}
)
@SessionFactory
public class CascadeToEmbeddedManyToOneTest {

	@AfterEach
	public void teaDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<CodedPairHolder> pairHolders = session.createQuery( "select p from CodedPairHolder p" ).list();
					pairHolders.forEach(
							pairHolder -> {
								PersonPair pair = pairHolder.getPair();
								session.remove( pairHolder );
								session.remove(pair.getLeft());
								session.remove(pair.getRight());
							}
					);
				}
		);
	}

	@Test
	public void testPersistCascadeToSetOfEmbedded(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Set<PersonPair> setOfPairs = new HashSet<>();
					setOfPairs.add( new PersonPair( new Person( "PERSON NAME 1" ), new Person( "PERSON NAME 2" ) ) );
					session.persist( new CodedPairSetHolder( "CODE", setOfPairs ) );
					session.flush();
				}
		);
	}

	@Test
	public void testPersistCascadeToEmbedded(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					PersonPair personPair = new PersonPair(
							new Person( "PERSON NAME 1" ),
							new Person( "PERSON NAME 2" )
					);
					session.persist( new CodedPairHolder( "CODE", personPair ) );
					session.flush();
				}
		);
	}
}
