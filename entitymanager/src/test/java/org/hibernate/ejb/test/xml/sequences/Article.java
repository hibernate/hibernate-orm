//$Id: $
package org.hibernate.ejb.test.xml.sequences;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.GeneratedValue;
import javax.persistence.CascadeType;
import javax.persistence.ManyToOne;
import javax.persistence.JoinColumn;

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
