//$Id$
package org.hibernate.test.annotations.indexcoll;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.MapKey;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Newspaper {
	private Integer id;
	private String name;
	private Map<String, News> news = new HashMap<String, News>();

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@ManyToMany
	@MapKey(name = "title")
	public Map<String, News> getNews() {
		return news;
	}

	public void setNews(Map<String, News> news) {
		this.news = news;
	}
}
