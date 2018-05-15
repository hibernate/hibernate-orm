/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.crud;

import java.util.List;

import org.hibernate.boot.MetadataSources;
import org.hibernate.orm.test.SessionFactoryBasedFunctionalTest;
import org.hibernate.orm.test.support.domains.gambit.Component;
import org.hibernate.orm.test.support.domains.gambit.EntityOfComposites;

import org.junit.jupiter.api.Test;

/**
 * Operation (save, load, etc) tests using EntityOfComposites
 *
 * @author Steve Ebersole
 */
public class EntityOfCompositesCrudTest extends SessionFactoryBasedFunctionalTest {
	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		metadataSources.addAnnotatedClass( EntityOfComposites.class );
	}

	@Override
	protected boolean exportSchema() {
		return true;
	}

	@Test
	public void testOperations() {
		sessionFactoryScope().inTransaction( session -> session.createQuery( "delete EntityOfComposites" ).executeUpdate() );

		final EntityOfComposites entity = new EntityOfComposites(
				1,
				new Component(
						"the string",
						2,
						4L,
						6,
						new Component.Nested(
								"the nested string",
								"the second nested string"
						)
				)
		);

		sessionFactoryScope().inTransaction( session -> session.save( entity ) );
		sessionFactoryScope().inTransaction(
				session -> {
					final String value = session.createQuery( "select s.component.basicString from EntityOfComposites s", String.class ).uniqueResult();
					assert "the string".equals( value );
				}
		);
		sessionFactoryScope().inTransaction(
				session -> {
					final EntityOfComposites loaded = session.get( EntityOfComposites.class, 1 );
					assert loaded != null;
					assert "the string".equals( loaded.getComponent().getBasicString() );
				}
		);
		sessionFactoryScope().inTransaction(
				session -> {
					final List<EntityOfComposites> list = session.byMultipleIds( EntityOfComposites.class )
							.multiLoad( 1, 2 );
					assert list.size() == 1;
					final EntityOfComposites loaded = list.get( 0 );
					assert loaded != null;
					assert "the string".equals( loaded.getComponent().getBasicString() );
				}
		);

		sessionFactoryScope().inTransaction(
				session -> {
					session.delete( session.find( EntityOfComposites.class, 1 ) );
				}
		);
	}
}
