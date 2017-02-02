/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria.basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
@Entity
@Table( name = "crit_basic_wall" )
public class Wall {
	private Long id;
	private long width;
	private long height;
	private String color;
	private Wall left;
	private Wall right;

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public long getWidth() {
		return width;
	}

	public void setWidth(long width) {
		this.width = width;
	}

	public long getHeight() {
		return height;
	}

	public void setHeight(long height) {
		this.height = height;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	@ManyToOne
	@JoinColumn(name = "left_id")
	public Wall getLeft() {
		return left;
	}

	public void setLeft(Wall left) {
		this.left = left;
	}

	@ManyToOne
	@JoinColumn(name = "right_id")
	public Wall getRight() {
		return right;
	}

	public void setRight(Wall right) {
		this.right = right;
	}
}
