/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.join;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.SQLInsert;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "Cat",
		indexes = {@Index(name = "secondname", columnList = "secondName"),
				@Index(name = "nameindex", columnList = "name")},
		comment = "My cat table")
@SecondaryTable(name = "`Cat nbr1`",
		indexes = @Index(name = "story1index", columnList = "storyPart1"))
@SecondaryTable(name = "Cat2",
		uniqueConstraints = @UniqueConstraint(columnNames = {"storyPart2"}),
		foreignKey = @ForeignKey(name="FK_CAT2_CAT"))
@SQLInsert(table = "Cat2", sql="insert into Cat2(storyPart2, id) values(upper(?), ?)")
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
