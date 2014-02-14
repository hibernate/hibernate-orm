//$Id$
package org.hibernate.test.annotations.join;
import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.SecondaryTable;
import javax.persistence.SecondaryTables;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.Table;
import org.hibernate.annotations.Tables;

/**
 * @author Emmanuel Bernard
 */
@Entity
@SecondaryTables({
@SecondaryTable(name = "`Cat nbr1`"),
@SecondaryTable(name = "Cat2", uniqueConstraints = {@UniqueConstraint(columnNames = {"storyPart2"})})
		})
@Tables( {
	@Table(comment = "My cat table", appliesTo = "Cat", indexes = {
			@Index(name = "secondname", columnList = "secondName"),
			@Index(name = "nameindex", columnList = "name"),
			@Index(name = "story1index", columnList = "`Cat nbr1`")}),
	@Table(appliesTo = "Cat2", foreignKey = @ForeignKey(name="FK_CAT2_CAT"), fetch = FetchMode.SELECT,
			sqlInsert=@SQLInsert(sql="insert into Cat2(storyPart2, id) values(upper(?), ?)"))})
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
