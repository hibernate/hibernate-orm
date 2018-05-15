/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.join;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SecondaryTable;
import javax.persistence.SecondaryTables;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@SecondaryTables({
@SecondaryTable(name = "`Cat nbr1`"),
@SecondaryTable(name = "Cat2", uniqueConstraints = {@UniqueConstraint(columnNames = {"storyPart2"})})
		})
@Table(appliesTo = "Cat", indexes = @Index(name = "secondname",
		columnNames = "secondName"), comment = "My cat table" )
@Table(appliesTo = "Cat2", foreignKey = @ForeignKey(name="FK_CAT2_CAT"), fetch = FetchMode.SELECT,
		sqlInsert=@SQLInsert(sql="insert into Cat2(storyPart2, id) values(upper(?), ?)") )
public class Cat implements Serializable {

	private Integer id;
	private String name;
	private String secondName;
	private String storyPart1;
	private String storyPart2;

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	@Index(name = "nameindex")
	public String getName() {
		return name;
	}

	public void setId(Integer integer) {
		id = integer;
	}

	public void setName(String string) {
		name = string;
	}

	public String getSecondName() {
		return secondName;
	}

	public void setSecondName(String secondName) {
		this.secondName = secondName;
	}

// Bug HHH-36
//	@OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
//	@JoinColumn(name="CAT_ID", secondaryTable="ExtendedLife")
//	public Set<Life> getLifes() {
//		return lifes;
//	}
//
//	public void setLifes(Set<Life> collection) {
//		lifes = collection;
//	}

	@Column(table = "`Cat nbr1`")
	@Index(name = "story1index")
	public String getStoryPart1() {
		return storyPart1;
	}

	@Column(table = "Cat2", nullable = false)
	public String getStoryPart2() {
		return storyPart2;
	}


	public void setStoryPart1(String string) {
		storyPart1 = string;
	}


	public void setStoryPart2(String string) {
		storyPart2 = string;
	}

}
