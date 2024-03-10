/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.insertordering;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import org.hibernate.testing.TestForIssue;
import org.junit.jupiter.api.Test;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-11714")
public class InsertOrderingWithSecondaryTable extends BaseInsertOrderingTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				ShapeEntity.class,
				ShapePolygonEntity.class,
				ShapeCircleEntity.class,
				GeographicArea.class,
				TopLevelEntity.class
		};
	}

	@Test
	public void testInheritanceWithSecondaryTable() {
		sessionFactoryScope().inTransaction( session -> {
			final TopLevelEntity top = new TopLevelEntity();

			final GeographicArea area1 = new GeographicArea();
			area1.setTopLevel( top );
			area1.setShape( new ShapePolygonEntity() );
			top.getGeographicAreas().add( area1 );

			final ShapeCircleEntity circle = new ShapeCircleEntity();
			circle.setCentre( "CENTRE" );

			final GeographicArea area2 = new GeographicArea();
			area2.setTopLevel( top );
			area2.setShape( circle );
			top.getGeographicAreas().add( area2 );

			session.save( top );

			clearBatches();
		} );

		verifyContainsBatches(
				new Batch( "insert into TOP_LEVEL (name,id) values (?,?)" ),
				new Batch( "insert into SHAPE (name,SHAPE_TYPE,SHAPE_ID) values (?," + literal( "POLYGON" ) + ",?)" ),
				new Batch( "insert into SHAPE (name,SHAPE_TYPE,SHAPE_ID) values (?," + literal( "CIRCLE" ) + ",?)" ),
				new Batch( "insert into SHAPE_CIRCLE (centre,SHAPE_ID) values (?,?)" ),
				new Batch( "insert into GEOGRAPHIC_AREA (name,SHAPE_ID,TOP_LEVEL_ID,id) values (?,?,?,?)", 2 )
		);
	}

	@Entity(name = "ShapeEntity")
	@Table(name = "SHAPE")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(name = "SHAPE_TYPE", discriminatorType = DiscriminatorType.STRING)
	public static class ShapeEntity {
		@Id
		@SequenceGenerator(name = "SHAPE_ID_GENERATOR", sequenceName = "SHAPE_SEQ", allocationSize = 1)
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SHAPE_ID_GENERATOR")
		@Column(name = "SHAPE_ID", insertable = false, updatable = false)
		private Long id;

		private String name;

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
	}

	@Entity(name = "ShapePolygonEntity")
	@DiscriminatorValue("POLYGON")
	public static class ShapePolygonEntity extends ShapeEntity {

	}

	@Entity(name = "ShapeCircleEntity")
	@DiscriminatorValue("CIRCLE")
	@SecondaryTable(name = "SHAPE_CIRCLE", pkJoinColumns = @PrimaryKeyJoinColumn(name = "SHAPE_ID"))
	public static class ShapeCircleEntity extends ShapeEntity {
		@Column(table = "SHAPE_CIRCLE")
		private String centre;

		public String getCentre() {
			return centre;
		}

		public void setCentre(String centre) {
			this.centre = centre;
		}
	}

	@Entity(name = "GeographicArea")
	@Table(name = "GEOGRAPHIC_AREA")
	public static class GeographicArea {
		@Id
		@GeneratedValue
		private Integer id;

		private String name;

		/// The reference to the top level class.
		@ManyToOne
		@JoinColumn(name = "TOP_LEVEL_ID")
		private TopLevelEntity topLevel;

		// The reference to the shape.
		@OneToOne(cascade = CascadeType.ALL)
		@JoinColumn(name = "SHAPE_ID")
		private ShapeEntity shape;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public TopLevelEntity getTopLevel() {
			return topLevel;
		}

		public void setTopLevel(TopLevelEntity topLevel) {
			this.topLevel = topLevel;
		}

		public ShapeEntity getShape() {
			return shape;
		}

		public void setShape(ShapeEntity shape) {
			this.shape = shape;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "TopLevelEntity")
	@Table(name = "TOP_LEVEL")
	public static class TopLevelEntity {
		@Id
		@GeneratedValue
		private Integer id;

		private String name;

		@OneToMany(mappedBy = "topLevel", cascade = { CascadeType.ALL }, orphanRemoval = true)
		private List<GeographicArea> geographicAreas = new ArrayList<>();

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public List<GeographicArea> getGeographicAreas() {
			return geographicAreas;
		}

		public void setGeographicAreas(List<GeographicArea> geographicAreas) {
			this.geographicAreas = geographicAreas;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
