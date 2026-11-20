package org.hibernate.orm.test.envers.integration.query;

import java.util.Collections;
import java.util.List;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.envers.query.AuditEntity;

import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.criteria.JoinType;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@code isNull()} on a to-one inside an embeddable must use that association,
 * not a same-named association on the root entity.
 */
@Jpa(annotatedClasses = {
		EmbeddableToOneIsNullQueryTest.Kind.class,
		EmbeddableToOneIsNullQueryTest.Item.class
})
@EnversTest
@JiraKey( value = "HHH-21212" )
public class EmbeddableToOneIsNullQueryTest {

	private static final int ROOT_KIND_UNSET = 1;
	private static final int DETAIL_KIND_UNSET = 2;

	@Embeddable
	public static class Detail {

		@ManyToOne
		@JoinColumn(name = "detail_kind_id")
		private Kind kind;

		private String label;

		public Detail() {
		}

		public Detail(Kind kind, String label) {
			this.kind = kind;
			this.label = label;
		}

		public Kind getKind() {
			return kind;
		}

		public String getLabel() {
			return label;
		}
	}

	@Entity(name = "Kind")
	@Audited
	public static class Kind {

		@Id
		private Integer id;

		private String name;

		public Kind() {
		}

		public Kind(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	@Entity(name = "Item")
	@Audited
	public static class Item {

		@Id
		private Integer id;

		@ManyToOne
		@JoinColumn(name = "root_kind_id")
		private Kind kind;

		@Embedded
		private Detail detail;

		public Item() {
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public void setKind(Kind kind) {
			this.kind = kind;
		}

		public void setDetail(Detail detail) {
			this.detail = detail;
		}
	}

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			Kind kind = new Kind( 1, "K" );
			em.persist( kind );

			Item rootUnset = new Item();
			rootUnset.setId( ROOT_KIND_UNSET );
			rootUnset.setDetail( new Detail( kind, "set" ) );
			em.persist( rootUnset );

			Item detailUnset = new Item();
			detailUnset.setId( DETAIL_KIND_UNSET );
			detailUnset.setKind( kind );
			detailUnset.setDetail( new Detail( null, "empty" ) );
			em.persist( detailUnset );
		} );
	}

	@Test
	public void isNullUsesTheEmbeddableAssociation(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List<?> nullDetails = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( Item.class, 1 )
					.traverseRelation( "detail", JoinType.INNER, "d" )
					.add( AuditEntity.property( "kind" ).isNull() )
					.up()
					.addProjection( AuditEntity.id() )
					.getResultList();
			assertEquals( Collections.singletonList( DETAIL_KIND_UNSET ), nullDetails );

			List<?> presentDetails = AuditReaderFactory.get( em ).createQuery()
					.forEntitiesAtRevision( Item.class, 1 )
					.traverseRelation( "detail", JoinType.INNER, "d" )
					.add( AuditEntity.property( "kind" ).isNotNull() )
					.up()
					.addProjection( AuditEntity.id() )
					.getResultList();
			assertEquals( Collections.singletonList( ROOT_KIND_UNSET ), presentDetails );
		} );
	}
}
