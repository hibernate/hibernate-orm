/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.test.naturalid;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.boot.MetadataSources;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
public class ValidationTests extends BaseUnitTestCase {
	@Test
	public void checkManyToOne() {
		final MetadataSources metadataSources = new MetadataSources()
				.addAnnotatedClass( Thing1.class )
				.addAnnotatedClass( Thing2.class );
		try (final SessionFactory sessionFactory = metadataSources.buildMetadata().buildSessionFactory(); ) {
			fail( "Expecting an exception" );
		}
		catch (MappingException expected) {
			assertThat( expected.getMessage() )
					.startsWith( "Attribute marked as natural-id can not also be a not-found association - " );
		}
	}

	@Test
	public void checkEmbeddable() {
		final MetadataSources metadataSources = new MetadataSources()
				.addAnnotatedClass( Thing1.class )
				.addAnnotatedClass( Thing3.class )
				.addAnnotatedClass( Container.class );
		try (final SessionFactory sessionFactory = metadataSources.buildMetadata().buildSessionFactory(); ) {
			fail( "Expecting an exception" );
		}
		catch (MappingException expected) {
			assertThat( expected.getMessage() )
					.startsWith( "Attribute marked as natural-id can not also be a not-found association - " );
		}
	}

	@Entity(name="Thing1")
	@Table(name="thing_1")
	public static class Thing1 {
		@Id
		private Integer id;
		private String name;
	}

	@Entity(name="Thing2")
	@Table(name="thing_2")
	public static class Thing2 {
		@Id
		private Integer id;
		private String name;
		@NaturalId
		@ManyToOne
		@NotFound(action = NotFoundAction.IGNORE)
		private Thing1 thing1;
	}

	@Embeddable
	public static class Container {
		@NaturalId
		@ManyToOne
		@NotFound(action = NotFoundAction.IGNORE)
		private Thing1 thing1;
	}

	@Entity(name="Thing2")
	@Table(name="thing_2")
	public static class Thing3 {
		@Id
		private Integer id;
		private String name;
		@NaturalId
		@Embedded
		private Container container;
	}
}
