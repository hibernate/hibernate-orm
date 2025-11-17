/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.merge;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				CompositeIdWithAssociationsAndGeneratedValuesMergeTest.Top.class,
				CompositeIdWithAssociationsAndGeneratedValuesMergeTest.Middle.class,
				CompositeIdWithAssociationsAndGeneratedValuesMergeTest.Bottom.class
		}
)
@SessionFactory
@JiraKey("HHH-16825")
public class CompositeIdWithAssociationsAndGeneratedValuesMergeTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testMerge(SessionFactoryScope scope) {
		String topName = "Top 1";

		scope.inTransaction(
				session -> {
					Top t1 = new Top( topName );
					session.persist( t1 );
				}
		);

		String middleName = "Middle";
		String bottomNote = "Bottom";
		Integer bottomType = 0;
		scope.inTransaction(
				session -> {
					Top top = session.createQuery( "from Top where name = :name", Top.class )
							.setParameter( "name", topName )
							.uniqueResult();

					Middle m1 = new Middle( middleName, top );
					new Bottom( m1, bottomType, bottomNote );

					session.merge( top );
				}
		);

		scope.inTransaction(
				session -> {
					Top top = session.createQuery( "from Top where name = :name", Top.class )
							.setParameter( "name", topName )
							.uniqueResult();
					assertTopIsAsExpected( top, middleName, bottomNote, bottomType );
				}
		);
	}

	@Test
	public void testPersist(SessionFactoryScope scope) {
		String topName = "Top 1";
		String middleName = "Middle";
		String bottomNote = "Bottom";
		Integer bottomType = 0;
		scope.inTransaction(
				session -> {
					Top top = new Top( topName );
					Middle m1 = new Middle( middleName, top );
					new Bottom( m1, bottomType, bottomNote );
					session.persist( top );
				}
		);

		scope.inTransaction(
				session -> {
					Top top = session.createQuery( "from Top where name = :name", Top.class )
							.setParameter( "name", topName )
							.uniqueResult();
					assertTopIsAsExpected( top, middleName, bottomNote, bottomType );
				}
		);
	}

	private static void assertTopIsAsExpected(Top top, String middleName, String bottomNote, Integer bottomType) {
		assertThat( top ).isNotNull();
		List<Middle> middles = top.getMiddles();
		assertThat( middles.size() ).isEqualTo( 1 );
		Middle middle = middles.get( 0 );
		assertThat( middle.getName() ).isEqualTo( middleName );

		List<Bottom> bottoms = middle.getBottoms();
		assertThat( bottoms.size() ).isEqualTo( 1 );
		Bottom bottom = bottoms.get( 0 );
		assertThat( bottom.getMiddle() ).isSameAs( middle );
		assertThat( bottom.getType() ).isEqualTo( bottomType );
		assertThat( bottom.getNote() ).isEqualTo( bottomNote );
	}


	@Entity(name = "Top")
	@Table(name = "top_table")
	public static class Top {
		private Long id;
		private String name;
		private List<Middle> middles;

		public Top() {
		}

		public Top(String name) {
			this.name = name;
		}

		@Id
		@GeneratedValue
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@OneToMany(mappedBy = "top", cascade = { CascadeType.MERGE, CascadeType.PERSIST })
		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		public List<Middle> getMiddles() {
			return middles;
		}

		public void setMiddles(List<Middle> middles) {
			this.middles = middles;
		}

		public void addMiddle(Middle middle) {
			if ( middles == null ) {
				middles = new ArrayList<>();
			}
			middles.add( middle );
		}
	}

	@Entity(name = "Middle")
	@Table(name = "middle_table")
	public static class Middle {
		private Long id;
		private String name;
		private Top top;
		private List<Bottom> bottoms;

		public Middle() {
		}

		public Middle(String name, Top top) {
			this.name = name;
			this.top = top;
			top.addMiddle( this );
		}

		@Id
		@GeneratedValue
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@ManyToOne(optional = false)
		@JoinColumn(name = "top_id", nullable = false)
		public Top getTop() {
			return top;
		}

		public void setTop(Top top) {
			this.top = top;
		}

		@OneToMany(mappedBy = "middle", cascade = { CascadeType.MERGE, CascadeType.PERSIST })
		public List<Bottom> getBottoms() {
			return bottoms;
		}

		public void setBottoms(List<Bottom> bottoms) {
			this.bottoms = bottoms;
		}

		public void addBottom(Bottom bottom) {
			if ( bottoms == null ) {
				bottoms = new ArrayList<>();
			}
			bottoms.add( bottom );
		}
	}

	@Entity(name = "Bottom")
	@Table(name = "bottom_table")
	public static class Bottom {
		private Middle middle;
		private Integer type;
		private String note;

		public Bottom() {
		}

		public Bottom(Middle middle, Integer type, String note) {
			this.middle = middle;
			this.middle.addBottom( this );
			this.type = type;
			this.note = note;
		}

		@Id
		@ManyToOne(optional = false)
		@JoinColumn(name = "middle_id", nullable = false)
		public Middle getMiddle() {
			return middle;
		}

		public void setMiddle(Middle middle) {
			this.middle = middle;
		}

		@Id
		@Column(name = "type_column")
		public Integer getType() {
			return type;
		}

		public void setType(Integer type) {
			this.type = type;
		}

		public String getNote() {
			return note;
		}

		public void setNote(String note) {
			this.note = note;
		}
	}
}
