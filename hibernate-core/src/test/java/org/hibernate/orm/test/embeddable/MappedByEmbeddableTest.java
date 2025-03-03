/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.embeddable;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.query.Query;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.Assertions.assertThat;

public class MappedByEmbeddableTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Containing.class, Embed.class, Contained.class };
	}

	@Test
	public void smoke() {
		inTransaction( session -> {
			saveComposition( session, 1, "data - 1" );
			saveComposition( session, 2, "data - 2" );
			saveComposition( session, 3, "data - 3" );
		} );

		inTransaction( session -> {
			queryContaining( session, 1, "data - 1" );
			queryContaining( session, 2, "data - 2" );
			queryContaining( session, 3, "data - 3" );
		} );

		inTransaction( session -> {
			loadContaining( session, 1, 1 );
			loadContaining( session, 2, 2 );
			loadContaining( session, 3, 3 );
		} );

		inTransaction( session -> {
			Containing containing1 = session.getReference( Containing.class, 1 );
			Containing containing2 = session.getReference( Containing.class, 2 );

			Embed embed1 = containing1.getEmbed();
			Embed embed2 = containing2.getEmbed();

			Contained contained1 = embed1.getContained();
			Contained contained2 = embed2.getContained();

			// switch associations: 1:1 2:2 -> 1:2 2:1
			contained1.setContaining( null );
			embed2.setContained( null );
			session.flush();
			embed1.setContained( contained2 );
			contained2.setContaining( containing1 );
			embed2.setContained( contained1 );
			contained1.setContaining( containing2 );
		} );

		inTransaction( session -> {
			queryContaining( session, 2, "data - 1" );
			queryContaining( session, 1, "data - 2" );
			queryContaining( session, 3, "data - 3" );
		} );

		inTransaction( session -> {
			loadContaining( session, 2, 1 );
			loadContaining( session, 1, 2 );
			loadContaining( session, 3, 3 );
		} );

		inTransaction( session -> {
			Query<Contained> query = session.createQuery( "select c from contained c", Contained.class );

			List<Contained> containeds = query.list();
			assertThat( containeds ).hasSize( 3 );
		} );
	}

	private void saveComposition(Session session, int id, String data) {
		Containing containing = new Containing();
		containing.setId( id );
		Contained contained = new Contained();
		contained.setId( id );
		contained.setData( data );
		Embed embed = new Embed();

		containing.setEmbed( embed );
		embed.setContained( contained );
		contained.setContaining( containing );

		session.persist( containing );
		session.persist( contained );
	}

	private void queryContaining(Session session, Integer id, String data) {
		Query<Containing> query = session.createQuery(
				"select c from containing c where c.embed.contained.data = :data",
				Containing.class
		);

		query.setParameter( "data", data );
		assertThat( query.getResultList() ).extracting( "id" ).containsExactly( id );
	}

	private void loadContaining(Session session, Integer containingId, Integer containedId) {
		Contained contained = session.getReference( Contained.class, containedId );
		Containing containing = contained.getContaining();

		assertThat( containing.getId() ).isEqualTo( containingId );
	}

	@Entity(name = "containing")
	public static class Containing {

		@Id
		private Integer id;

		@Embedded
		private Embed embed = new Embed();

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Embed getEmbed() {
			return embed;
		}

		public void setEmbed(Embed embed) {
			this.embed = embed;
		}
	}

	@Embeddable
	public static class Embed {

		@OneToOne
		private Contained contained;

		public Contained getContained() {
			return contained;
		}

		public void setContained(Contained contained) {
			this.contained = contained;
		}
	}

	@Entity(name = "contained")
	public static class Contained {

		@Id
		private Integer id;

		@Basic
		private String data;

		@OneToOne(mappedBy = "embed.contained")
		private Containing containing;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}

		public Containing getContaining() {
			return containing;
		}

		public void setContaining(Containing containing) {
			this.containing = containing;
		}
	}
}
