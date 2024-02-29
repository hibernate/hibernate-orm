/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.jpa.query;

import java.time.LocalDate;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Jan Schatteman
 */
@Jpa(
		annotatedClasses = { TupleSubqueryTest.MarketSale.class, TupleSubqueryTest.Peach.class }
)
public class TupleSubqueryTest {

	@Test
	public void testTupleSubquery(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					final String fruitName = "Good Peach";
					final FruitColor fruitColor = FruitColor.RED;

					Peach peach = new Peach();
					peach.setColor(fruitColor);
					peach.setName(fruitName);
					entityManager.persist(peach);

					MarketSale marketSale = new MarketSale();
					marketSale.setDateSold(LocalDate.now());
					marketSale.setFruitColor(fruitColor);
					marketSale.setFruitName(fruitName);
					entityManager.persist(marketSale);

					int count = entityManager
							.createQuery(
									"DELETE FROM MarketSale m " +
											"WHERE (m.fruitColor, m.fruitName) IN (SELECT p.color, p.name FROM Peach p)")
							.executeUpdate();
					assertEquals(1, count);
				}
		);
	}

	public enum FruitColor
	{
		RED,
		GREEN,
		ORANGE,
		YELLOW
	}

	@Entity(name = "MarketSale")
	public static class MarketSale
	{
		@Id
		@GeneratedValue
		private Integer id;
		private String fruitName;
		@Enumerated(EnumType.STRING)
		private FruitColor fruitColor;
		private LocalDate dateSold;
		public Integer getId()
		{
			return id;
		}
		public void setId(Integer id)
		{
			this.id = id;
		}
		public String getFruitName()
		{
			return fruitName;
		}
		public void setFruitName(String fruitName)
		{
			this.fruitName = fruitName;
		}
		public FruitColor getFruitColor()
		{
			return fruitColor;
		}
		public void setFruitColor(FruitColor fruitColor)
		{
			this.fruitColor = fruitColor;
		}
		public LocalDate getDateSold()
		{
			return dateSold;
		}
		public void setDateSold(LocalDate dateSold)
		{
			this.dateSold = dateSold;
		}
	}

	@Entity(name = "Peach")
	public static class Peach
	{
		@Id
		@GeneratedValue
		private Integer id;
		private String name;
		@Enumerated(EnumType.STRING)
		private FruitColor color;
		public Integer getId()
		{
			return id;
		}
		public void setId(Integer id)
		{
			this.id = id;
		}
		public String getName()
		{
			return name;
		}
		public void setName(String name)
		{
			this.name = name;
		}
		public FruitColor getColor()
		{
			return color;
		}
		public void setColor(FruitColor color)
		{
			this.color = color;
		}
	}

}
