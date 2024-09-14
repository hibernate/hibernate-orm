/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.indexcoll;
import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapKey;

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
