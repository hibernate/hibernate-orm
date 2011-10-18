//$Id$
package org.hibernate.test.annotations.backquotes;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Category 
{
	@Id
	@Column(name="`cat_id`")
	private int id;
	
	@Column(name="`title`")
	private String title;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
}
