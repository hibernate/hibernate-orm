package org.hibernate.orm.test.proxy;

import jakarta.persistence.*;
import org.hibernate.annotations.Proxy;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Oliver Henlich
 */
/**
 * Test that demonstrates the intermittent {@link ClassCastException} that occurs
 * when trying to call a method on a {@link HibernateProxy} where the parameter
 * type is defined by generics.
 */
@JiraKey("HHH-17578")
@DomainModel(
		annotatedClasses = {
				ProxyWithGenericsTest.AbstractEntityImpl.class,
				ProxyWithGenericsTest.AbstractShapeEntityImpl.class,
				ProxyWithGenericsTest.CircleEntityImpl.class,
				ProxyWithGenericsTest.SquareEntityImpl.class,
				ProxyWithGenericsTest.CircleContainerEntityImpl.class,
				ProxyWithGenericsTest.SquareContainerEntityImpl.class,
				ProxyWithGenericsTest.MainEntityImpl.class
		}
)
@SessionFactory
@SuppressWarnings("ALL")
public class ProxyWithGenericsTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			EntityManager em = session.unwrap(EntityManager.class);

			// Shape 1
			CircleEntityImpl cirlce1 = new CircleEntityImpl();
			cirlce1.radius = BigDecimal.valueOf(1);
			em.persist(cirlce1);

			// Shape 2
			SquareEntityImpl square1 = new SquareEntityImpl();
			square1.width = BigDecimal.valueOf(1);
			square1.height = BigDecimal.valueOf(2);
			em.persist(square1);

			// Container 1
			CircleContainerEntity circleContainer1 = new CircleContainerEntityImpl();
			em.persist(circleContainer1);

			// Container 2
			SquareContainerEntity squareContainer1 = new SquareContainerEntityImpl();
			em.persist(squareContainer1);

			// Main
			MainEntityImpl main = new MainEntityImpl();
			main.circleContainer = circleContainer1;
			main.squareContainer = squareContainer1;
			em.persist(main);

		});
	}

	@Test
	void test(SessionFactoryScope scope) throws Exception {

		scope.inTransaction(session -> {
			EntityManager em = session.unwrap(EntityManager.class);

			MainEntityImpl main = em.find(MainEntityImpl.class, 1L);
			assertNotNull(main);

			CircleContainerEntity circleContainer = main.getCircleContainer();
			assertNotNull(circleContainer);
			assertTrue(circleContainer instanceof HibernateProxy);
			CircleEntity circle1 = em.find(CircleEntityImpl.class, 1L);
			circleContainer.add(circle1); // This method fails with ClassCastException without the fix

			SquareContainerEntity squareContainer = main.getSquareContainer();
			assertNotNull(squareContainer);
			assertTrue(squareContainer instanceof HibernateProxy);
			SquareEntity square1 = em.find(SquareEntityImpl.class, 2L);
			squareContainer.add(square1); // This method fails with ClassCastException without the fix
		});
	}

	// Shapes hierarchy -------------------------------------------------------
	public interface ShapeEntity {
		BigDecimal getArea();
	}

	public interface CircleEntity extends ShapeEntity {
		BigDecimal getRadius();
	}

	public interface SquareEntity extends ShapeEntity {
		BigDecimal getWidth();

		BigDecimal getHeight();
	}

	@MappedSuperclass
	@Access(AccessType.FIELD)
	public static abstract class AbstractEntityImpl {
		@Id
		@GeneratedValue
		@Column(name = "ID")
		Long id;
	}


	@Entity
	@Table(name = "SHAPE")
	@Access(AccessType.FIELD)
	@Proxy(proxyClass = ShapeEntity.class)
	@DiscriminatorColumn(name = "TYPE", length = 20)
	public static abstract class AbstractShapeEntityImpl
			extends AbstractEntityImpl
			implements ShapeEntity {
		public abstract BigDecimal getArea();
	}

	@Entity
	@Proxy(proxyClass = CircleEntity.class)
	@Access(AccessType.FIELD)
	@DiscriminatorValue("CIRCLE")
	public static class CircleEntityImpl
			extends AbstractShapeEntityImpl
			implements CircleEntity {
		@Column(name = "RADIUS", nullable = true)
		private BigDecimal radius;

		@Override
		public BigDecimal getArea() {
			return new BigDecimal(Math.PI).multiply(radius.pow(2));
		}

		@Override
		public BigDecimal getRadius() {
			return radius;
		}
	}

	@Entity
	@Proxy(proxyClass = SquareEntity.class)
	@Access(AccessType.FIELD)
	@DiscriminatorValue("SQUARE")
	public static class SquareEntityImpl
			extends AbstractShapeEntityImpl
			implements SquareEntity {

		@Column(name = "WIDTH", nullable = true)
		private BigDecimal width;

		@Column(name = "HEIGHT", nullable = true)
		private BigDecimal height;

		@Override
		public BigDecimal getArea() {
			return width.multiply(height);
		}

		@Override
		public BigDecimal getWidth() {
			return width;
		}

		@Override
		public BigDecimal getHeight() {
			return height;
		}
	}

	// ShapeContainer hierarchy -----------------------------------------------

	public interface ShapeContainerEntity<T extends ShapeEntity> {
		void add(T shape);
	}

	public interface CircleContainerEntity extends ShapeContainerEntity<CircleEntity> {

	}

	public interface SquareContainerEntity extends ShapeContainerEntity<SquareEntity> {

	}


	@Entity
	@Table(name = "CONTAINER")
	@Access(AccessType.FIELD)
	@Proxy(proxyClass = ShapeContainerEntity.class)
	@DiscriminatorColumn(name = "TYPE", length = 20)
	public static abstract class AbstractShapeContainerEntityImpl<T extends ShapeEntity>
			extends AbstractEntityImpl
			implements ShapeContainerEntity<T> {
	}



	@Entity
	@Proxy(proxyClass = SquareContainerEntity.class)
	@Access(AccessType.FIELD)
	@DiscriminatorValue("SQUARE")
	public static class SquareContainerEntityImpl
			extends AbstractShapeContainerEntityImpl<SquareEntity>
			implements SquareContainerEntity {

		@Override
		public void add(SquareEntity shape) {
		}
	}

	@Entity
	@Proxy(proxyClass = CircleContainerEntity.class)
	@Access(AccessType.FIELD)
	@DiscriminatorValue("CIRCLE")
	public static class CircleContainerEntityImpl
			extends AbstractShapeContainerEntityImpl<CircleEntity>
			implements CircleContainerEntity {

		@Override
		public void add(CircleEntity shape) {
		}
	}

	/**
	 * Main test entity that has lazy references to the two types of {@link ShapeContainerEntity containers}.
	 */
	@Entity
	@Table(name = "Main")
	@Access(AccessType.FIELD)
	public static class MainEntityImpl
			extends AbstractEntityImpl {

		@ManyToOne(targetEntity = AbstractShapeContainerEntityImpl.class, optional = true, fetch = FetchType.LAZY)
		@JoinColumn(name = "CIRCLE_CONTAINER_ID")
		private CircleContainerEntity circleContainer;

		@ManyToOne(targetEntity = AbstractShapeContainerEntityImpl.class, optional = true, fetch = FetchType.LAZY)
		@JoinColumn(name = "SQUARE_CONTAINER_ID")
		private SquareContainerEntity squareContainer;

		public CircleContainerEntity getCircleContainer() {
			return circleContainer;
		}

		public SquareContainerEntity getSquareContainer() {
			return squareContainer;
		}
	}
}