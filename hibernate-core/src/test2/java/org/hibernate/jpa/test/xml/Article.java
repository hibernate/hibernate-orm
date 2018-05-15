/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.jpa.test.xml;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "ITEM")
@org.hibernate.annotations.BatchSize(size = 10)
public class Article {
	private Integer id;
	private String name;

	private Article nextArticle;

	@Id @GeneratedValue public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	@Column(name="poopoo")
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}


	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "NEXT_MESSAGE_ID")
	public Article getNextArticle() {
		return nextArticle;
	}

	public void setNextArticle(Article nextArticle) {
		this.nextArticle = nextArticle;
	}
}
