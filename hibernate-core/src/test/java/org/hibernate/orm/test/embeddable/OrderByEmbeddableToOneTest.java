/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.embeddable;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Basic;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderBy;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.query.Query;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.AvailableSettings.DEFAULT_LIST_SEMANTICS;

public class OrderByEmbeddableToOneTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Containing.class, Embed.class, Contained.class };
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.setProperty( DEFAULT_LIST_SEMANTICS, CollectionClassification.BAG );
	}

	@Test
	public void test() {
		inTransaction( session -> {
			saveComposition( session, 1 );
			saveComposition( session, 11 );
			saveComposition( session, 21 );
		} );

		inTransaction( session -> {
			Query<Containing> query = session.createQuery( "select c from containing c order by c.id asc", Containing.class );

			List<Containing> resultList = query.getResultList();
			assertThat( resultList ).hasSize( 3 );
			assertThat( resultList.get( 0 ).getEmbeds() )
					.extracting( "contained" ).extracting( "id" ).containsExactly( 3, 2, 1 );
			assertThat( resultList.get( 1 ).getEmbeds() )
					.extracting( "contained" ).extracting( "id" ).containsExactly( 13, 12, 11 );
			assertThat( resultList.get( 2 ).getEmbeds() )
					.extracting( "contained" ).extracting( "id" ).containsExactly( 23, 22, 21 );
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
		contained1.setId( id );
		contained1.setData( "data" + id++ );
		Contained contained2 = new Contained();
		contained2.setId( id );
		contained2.setData( "data" + id++ );
		Contained contained3 = new Contained();
		contained3.setId( id );
		contained3.setData( "data" + id++ );

		embed2.setContained( contained2 );
		embed3.setContained( contained3 );
		embed1.setContained( contained1 );

		session.persist( containing );
		session.persist( contained1 );
		session.persist( contained2 );
		session.persist( contained3 );
	}

	@Entity(name = "containing")
	public static class Containing {

		@Id
		private Integer id;

		@ElementCollection
		@OrderBy("contained.id desc")
		private List<Embed> embeds = new ArrayList<>();

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

		@ManyToOne
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
	}

}
