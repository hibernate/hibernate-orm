/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.embeddable;

import jakarta.persistence.Basic;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OrderBy;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {
		OrderByEmbeddableX2Test.Containing.class,
		OrderByEmbeddableX2Test.Embed.class,
		OrderByEmbeddableX2Test.Contained.class
})
@SessionFactory
public class OrderByEmbeddableX2Test {
	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void test(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
			saveComposition( session, 1 );
			saveComposition( session, 11 );
			saveComposition( session, 21 );
		} );

		factoryScope.inTransaction( session -> {
			Query<Containing> query = session.createQuery( "select c from containing c order by c.id asc", Containing.class );

			List<Containing> resultList = query.getResultList();
			assertThat( resultList ).hasSize( 3 );
			assertThat( resultList.get( 0 ).getEmbeds() )
					.extracting( "contained" ).extracting( "data" ).containsExactly( "data3", "data2", "data1" );
			assertThat( resultList.get( 1 ).getEmbeds() )
					.extracting( "contained" ).extracting( "data" ).containsExactly( "data13", "data12", "data11" );
			assertThat( resultList.get( 2 ).getEmbeds() )
					.extracting( "contained" ).extracting( "data" ).containsExactly( "data23", "data22", "data21" );
		} );
	}

	private void saveComposition(Session session, int id) {
		Containing containing = new Containing();
		containing.setId( id );

		Embed embed1 = new Embed();
		Embed embed2 = new Embed();
		Embed embed3 = new Embed();
		ArrayList<Embed> embeds = new ArrayList<>();
		embeds.add( embed1 );
		embeds.add( embed2 );
		embeds.add( embed3 );
		containing.setEmbeds( embeds );

		Contained contained1 = new Contained();
		contained1.setData( "data" + id++ );
		Contained contained2 = new Contained();
		contained2.setData( "data" + id++ );
		Contained contained3 = new Contained();
		contained3.setData( "data" + id++ );

		embed1.setContained( contained1 );
		embed2.setContained( contained2 );
		embed3.setContained( contained3 );

		session.persist( containing );
	}

	@Entity(name = "containing")
	public static class Containing {

		@Id
		private Integer id;

		@ElementCollection
		@OrderBy("contained.data desc")
		private List<Embed> embeds = new ArrayList<>();

		public Containing() {
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public List<Embed> getEmbeds() {
			return embeds;
		}

		public void setEmbeds(List<Embed> embeds) {
			this.embeds = embeds;
		}
	}

	@Embeddable
	public static class Embed {

		@Embedded
		private Contained contained;

		public Embed() {
		}

		public Contained getContained() {
			return contained;
		}

		public void setContained(Contained contained) {
			this.contained = contained;
		}
	}

	@Embeddable
	public static class Contained {

		@Basic
		private String data;

		public Contained() {
		}

		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}
	}
}
