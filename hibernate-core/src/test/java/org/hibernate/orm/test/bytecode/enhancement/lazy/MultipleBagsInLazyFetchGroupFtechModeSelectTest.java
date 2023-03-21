/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.engine.spi.SessionImplementor;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Christian Beikov
 */
@RunWith(BytecodeEnhancerRunner.class)
public class MultipleBagsInLazyFetchGroupFtechModeSelectTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { StringsEntity.class };
	}

	@Override
	protected void configureSessionFactoryBuilder(SessionFactoryBuilder sfb) {
		super.configureSessionFactoryBuilder( sfb );
		sfb.applyCollectionsInDefaultFetchGroup( true );
	}

	@After
	public void tearDown() {
		doInJPA( this::sessionFactory, em -> {
			em.createQuery( "delete from StringsEntity" ).executeUpdate();
		} );
	}

	@Before
	public void prepare() {
		assertTrue( sessionFactory().getSessionFactoryOptions().isCollectionsInDefaultFetchGroupEnabled() );

		doInJPA( this::sessionFactory, em -> {
			StringsEntity entity = new StringsEntity();
			entity.id = 1L;
			entity.text = "abc";
			entity.someStrings = new ArrayList<>( Arrays.asList( "a", "b", "c" ) );
			entity.someStrings2 = new ArrayList<>( Arrays.asList( "a", "b", "c", "d" ) );
			em.persist( entity );
		} );
	}

	@Test
	public void testGetReference() {
		Assertions.assertTrue( sessionFactory().getSessionFactoryOptions().isCollectionsInDefaultFetchGroupEnabled() );
		doInJPA( this::sessionFactory, entityManager -> {
			StringsEntity entity = entityManager.getReference( StringsEntity.class, 1L );
			assertEquals( 3, entity.someStrings.size() );
			assertEquals( 4, entity.someStrings2.size() );
		} );
	}

	@Test
	public void testFind() {
		Assertions.assertTrue( sessionFactory().getSessionFactoryOptions().isCollectionsInDefaultFetchGroupEnabled() );
		doInJPA( this::sessionFactory, entityManager -> {
			StringsEntity entity = entityManager.find( StringsEntity.class, 1L );
			assertEquals( 3, entity.someStrings.size() );
			assertEquals( 4, entity.someStrings2.size() );

		} );
	}

	// --- //

	@Entity(name = "StringsEntity")
	@Table(name = "STRINGS_ENTITY")
	private static class StringsEntity {

		@Id
		Long id;

		String text;

		@ElementCollection(fetch = FetchType.EAGER)
		@Fetch(FetchMode.SELECT)
		List<String> someStrings;

		@ElementCollection(fetch = FetchType.EAGER)
		@Fetch(FetchMode.SELECT)
		List<String> someStrings2;
	}
}
