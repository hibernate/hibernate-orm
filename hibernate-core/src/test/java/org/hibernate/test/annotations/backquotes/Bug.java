//$Id$
package org.hibernate.test.annotations.backquotes;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

@Entity
@Table(indexes = @Index(name="`titleindex`", columnList = "`title`"))
public class Bug 
{
	@Id
	@Column(name="`bug_id`")
	private int id;
	
	@Column(name="`title`")
	private String title;
	
	@ManyToMany
	@JoinTable(name="`bug_category`")
	private List<Category> categories;

	public List<Category> getCategories() {
		return categories;
	}

	public void setCategories(List<Category> categories) {
		this.categories = categories;
	}

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
