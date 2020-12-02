/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.cascade;

import java.util.ArrayList;
import java.util.Collection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.junit.jupiter.api.Test;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import static javax.persistence.CascadeType.DETACH;
import static javax.persistence.CascadeType.REMOVE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Emmanuel Bernard
 */
@DomainModel(annotatedClasses = {
		DetachAndContainsTest.Mouth.class,
		DetachAndContainsTest.Tooth.class
})
@SessionFactory
public class DetachAndContainsTest {
	@Test
	public void testDetach(SessionFactoryScope scope) {
		Tooth tooth = new Tooth();
		Mouth mouth = new Mouth();
		scope.inTransaction(
				session -> {
					session.persist( mouth );
					session.persist( tooth );
					tooth.mouth = mouth;
					mouth.teeth = new ArrayList<>();
					mouth.teeth.add( tooth );
				}
		);

		scope.inTransaction(
				session -> {
					Mouth _mouth = session.find( Mouth.class, mouth.id );
					assertNotNull( _mouth );
					assertEquals( 1, _mouth.teeth.size() );
					Tooth _tooth = _mouth.teeth.iterator().next();
					session.detach( _mouth );
					assertFalse( session.contains( _tooth ) );
				}
		);

		scope.inTransaction(
				session -> session.remove( session.find( Mouth.class, mouth.id ) )
		);
	}

	@Entity
	@Table(name = "mouth")
	public static class Mouth {
		@Id
		@GeneratedValue
		public Integer id;
		@OneToMany(mappedBy = "mouth", cascade = { DETACH, REMOVE } )
		public Collection<Tooth> teeth;
	}

	@Entity
	@Table(name = "tooth")
	public static class Tooth {
		@Id
		@GeneratedValue
		public Integer id;
		public String type;
		@ManyToOne
		public Mouth mouth;
	}
}
