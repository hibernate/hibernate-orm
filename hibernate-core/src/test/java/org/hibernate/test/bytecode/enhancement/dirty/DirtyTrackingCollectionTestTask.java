/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.dirty;

import java.util.ArrayList;
import java.util.Arrays;

import org.hibernate.cfg.Configuration;

import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Luis Barreiro
 */
public class DirtyTrackingCollectionTestTask extends AbstractEnhancerTestTask {

	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {ParentChildEntity.class};
	}

	public void prepare() {
		super.prepare( new Configuration() );
	}

	public void execute() {

		doInJPA( this::getFactory, entityManager -> {
			ParentChildEntity entity = new ParentChildEntity();
			entity.setId( 1L );
			entity.setSomeStrings( new ArrayList<>( Arrays.asList( "a", "b", "c") ) );
			entityManager.persist( entity );
		} );

		doInJPA( this::getFactory, entityManager -> {
			ParentChildEntity entity = entityManager.find( ParentChildEntity.class, 1L );
			entity.getSomeStrings().clear();
		} );

		doInJPA( this::getFactory, entityManager -> {
			ParentChildEntity entity = entityManager.find( ParentChildEntity.class, 1L );
			assertEquals(0, entity.getSomeStrings().size());
			entity.getSomeStrings().add( "d" );
		} );

		doInJPA( this::getFactory, entityManager -> {
			ParentChildEntity entity = entityManager.find( ParentChildEntity.class, 1L );
			assertEquals(1, entity.getSomeStrings().size());
			entity.setSomeStrings( new ArrayList<>() );
		} );

		doInJPA( this::getFactory, entityManager -> {
			ParentChildEntity entity = entityManager.find( ParentChildEntity.class, 1L );
			assertEquals(0, entity.getSomeStrings().size());
		} );
	}

	protected void cleanup() {
	}
}
