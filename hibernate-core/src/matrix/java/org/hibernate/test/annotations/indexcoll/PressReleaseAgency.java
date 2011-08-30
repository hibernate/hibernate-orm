//$Id$
package org.hibernate.test.annotations.indexcoll;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MapKey;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class PressReleaseAgency {
	private Integer id;
	private String name;
	private Map<Integer, News> providedNews = new HashMap<Integer, News>();

	@Id
	@GeneratedValue
	@Column(name = "PressReleaseAgency_id")
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
	@JoinTable(joinColumns = @JoinColumn(name = "PressReleaseAgency_id"),
			inverseJoinColumns = @JoinColumn(name = "News_id"))
	@MapKey
	public Map<Integer, News> getProvidedNews() {
		return providedNews;
	}

	public void setProvidedNews(Map<Integer, News> providedNews) {
		this.providedNews = providedNews;
	}
}
