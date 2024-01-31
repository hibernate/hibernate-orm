package org.hibernate.orm.test.merge;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
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
				CompositeIdWithAssociationsAndGeneratedValuesMerge2Test.Middle.class,
				CompositeIdWithAssociationsAndGeneratedValuesMerge2Test.Bottom.class
		}
)
@SessionFactory
@JiraKey( "HHH-17634" )
public class CompositeIdWithAssociationsAndGeneratedValuesMerge2Test {

	@Test
	public void testMerge(SessionFactoryScope scope) {

		scope.inTransaction(
				session -> {
					Middle m1 = new Middle( "Middle" );
					Bottom bottom = new Bottom( m1, 0, "Bottom" );
					Middle merge = session.merge( m1 );
					assertThat( merge.getId() ).isNotNull();
					assertThat( m1.getId() ).isNull();
				}
		);
	}

	@Entity(name = "Middle")
	@Table(name = "middle_table")
	public static class Middle {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@OneToMany(mappedBy = "middle", cascade = { CascadeType.MERGE, CascadeType.PERSIST })
		private List<Bottom> bottoms;

		public Middle() {
		}

		public Middle(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}


		public String getName() {
			return name;
		}


		public List<Bottom> getBottoms() {
			return bottoms;
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
		@Id
		@ManyToOne(optional = false, cascade = CascadeType.MERGE)
		@JoinColumn(name = "middle_id", nullable = false)
		private Middle middle;

		@Id
		@Column(name = "type_column")
		private Integer type;

		private String note;

		public Bottom() {
		}

		public Bottom(Middle middle, Integer type,String note) {
			this.middle = middle;
			this.middle.addBottom( this );
			this.type = type;
			this.note = note;
		}
	}

}
