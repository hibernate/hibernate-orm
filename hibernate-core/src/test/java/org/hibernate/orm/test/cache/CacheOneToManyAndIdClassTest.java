/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static jakarta.persistence.CascadeType.ALL;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				CacheOneToManyAndIdClassTest.Content.class,
				CacheOneToManyAndIdClassTest.Detail.class
		}
)
@SessionFactory(generateStatistics = true)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.USE_QUERY_CACHE, value = "true"),
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true")
		}
)
@JiraKey(value = "HHH-16493")
public class CacheOneToManyAndIdClassTest {

	public static final Long CONTENT_ID = 200l;
	public static final String CONTENT_NAME = "Important";

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Content content = new Content( CONTENT_ID, CONTENT_NAME );
					Detail detail = new Detail( 300l, 400l, "detail" );
					content.addDetail( detail );
					session.persist( content );
					session.persist( detail );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope){
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testIt(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Content content = session.get( Content.class, PkComposite.withId( CONTENT_ID ) );
					assertThat( content ).isNotNull();
					assertThat( content.getName() ).isEqualTo( CONTENT_NAME );
					List<Detail> details = content.getDetailList();
					assertThat( details.size() ).isEqualTo( 1 );
				}
		);

		scope.inTransaction(
				session -> {
					Content content = session.get( Content.class, PkComposite.withId( CONTENT_ID ) );
					assertThat( content ).isNotNull();
					assertThat( content.getName() ).isEqualTo( CONTENT_NAME );
					List<Detail> details = content.getDetailList();
					assertThat( details.size() ).isEqualTo( 1 );

					details.remove( details.get( 0 ) );

					Detail newDetail = new Detail( 301l, 901l, "New detail" );
					content.addDetail( newDetail );
				}
		);

		scope.inTransaction(
				session -> {
					Content content = session.get( Content.class, PkComposite.withId( CONTENT_ID ) );
					assertThat( content ).isNotNull();
					assertThat( content.getName() ).isEqualTo( CONTENT_NAME );
					List<Detail> details = content.getDetailList();
					assertThat( details.size() ).isEqualTo( 1 );

					assertThat( details.get( 0 ).getId() ).isEqualTo( 301l );
					assertThat( details.get( 0 ).getId2() ).isEqualTo( 901l );
					assertThat( details.get( 0 ).getName() ).isEqualTo( "New detail" );

				}
		);
	}

	@Test
	public void testIt2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Content content = session.get( Content.class, PkComposite.withId( CONTENT_ID ) );
					assertThat( content ).isNotNull();
					assertThat( content.getName() ).isEqualTo( CONTENT_NAME );
					List<Detail> details = content.getDetailList();
					assertThat( details.size() ).isEqualTo( 1 );

					details.remove( details.get( 0 ) );

					Detail newDetail = new Detail( 301l, 901l, "New detail" );
					content.addDetail( newDetail );
				}
		);

		scope.inTransaction(
				session -> {
					Content content = session.get( Content.class, PkComposite.withId( CONTENT_ID ) );
					assertThat( content ).isNotNull();
					assertThat( content.getName() ).isEqualTo( CONTENT_NAME );
					List<Detail> details = content.getDetailList();
					assertThat( details.size() ).isEqualTo( 1 );

					assertThat( details.get( 0 ).getId() ).isEqualTo( 301l );
					assertThat( details.get( 0 ).getId2() ).isEqualTo( 901l );
					assertThat( details.get( 0 ).getName() ).isEqualTo( "New detail" );

				}
		);
	}


	@Entity(name = "Content")
	@Table(name = "CONTENT_TABLE")
	public static class Content {

		@EmbeddedId
		private PkComposite id;

		private String name;

		@OneToMany(cascade = ALL, orphanRemoval = true)
		@JoinColumn(name = "ID_CONTENT", referencedColumnName = "ID")
		private List<Detail> detailList = new ArrayList<>();

		public Content() {
		}

		public Content(Long id, String name) {
			this.id = PkComposite.withId( id );
			this.name = name;
		}

		public void addDetail(Detail detail) {
			detailList.add( detail );
		}

		public PkComposite getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public List<Detail> getDetailList() {
			return detailList;
		}
	}

	public static class PkComposite implements Serializable {

		@Column(name = "ID")
		private Long id;

		public PkComposite() {
		}

		public PkComposite(Long id) {
			this.id = id;
		}

		public static PkComposite withId(Long id) {

			return new PkComposite( id );
		}
	}

	public static class DoubleId implements Serializable {
		private Long id;
		private Long id2;

		public DoubleId() {
		}

		public DoubleId(Long id, Long id2) {
			this.id = id;
			this.id2 = id2;
		}

	}

	@Entity(name = "Detail")
	@IdClass(DoubleId.class)
	@Table(name = "DETAIL_TABLE")
	public static class Detail {

		@Id
		private Long id;
		@Id
		private Long id2;

		private String name;

		public Detail() {
		}

		public Detail(Long id, Long id2, String name) {
			this.id = id;
			this.id2 = id2;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public Long getId2() {
			return id2;
		}

		public String getName() {
			return name;
		}
	}
}
