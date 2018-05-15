/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.insertordering;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-11714")
public class InsertOrderingWithSecondaryTable extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		options.put( AvailableSettings.ORDER_INSERTS, Boolean.TRUE );
		options.put( AvailableSettings.ORDER_UPDATES, Boolean.TRUE );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{
				ShapeEntity.class,
				ShapePolygonEntity.class,
				ShapeCircleEntity.class,
				GeographicArea.class,
				TopLevelEntity.class
		};
	}

	@Test
	public void testInheritanceWithSecondaryTable() {
		doInJPA( this::entityManagerFactory, entityManager -> {
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

			entityManager.persist( top );
			entityManager.flush();
		} );
	}

	@Entity
	@Table(name = "SHAPE")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(name = "SHAPE_TYPE", discriminatorType = DiscriminatorType.STRING)
	public static class ShapeEntity {
		@Id
		@SequenceGenerator(name = "SHAPE_ID_GENERATOR", sequenceName = "SHAPE_SEQ", allocationSize = 1)
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SHAPE_ID_GENERATOR")
		@Column(name = "SHAPE_ID", insertable = false, updatable = false)
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@Entity
	@DiscriminatorValue("POLYGON")
	@Table(name = "POLYGON")
	public static class ShapePolygonEntity extends ShapeEntity {

	}

	@Entity
	@DiscriminatorValue("CIRCLE")
	@SecondaryTable(name = "SHAPE_CIRCLE", pkJoinColumns = @PrimaryKeyJoinColumn(name = "SHAPE_ID"))
	@Table(name = "CIRCLE")
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

	@Entity
	@Table(name = "GEOGRAPHIC_AREA")
	public static class GeographicArea {
		@Id
		@GeneratedValue
		private Integer id;

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
	}

	@Entity
	@Table(name = "TOP_LEVEL")
	public static class TopLevelEntity {
		@Id
		@GeneratedValue
		private Integer id;

		@OneToMany(mappedBy = "topLevel", cascade = {CascadeType.ALL},  orphanRemoval = true)
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
	}
}