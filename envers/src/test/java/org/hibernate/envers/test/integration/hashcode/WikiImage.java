package org.hibernate.envers.test.integration.hashcode;

import org.hibernate.envers.Audited;

import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.Entity;
import javax.persistence.Basic;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Audited
public class WikiImage {
	@Id
	@GeneratedValue
	private Long id;

	@Basic
	private String name;

	public WikiImage() {
	}

	public WikiImage(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof WikiImage)) return false;

		WikiImage wikiImage = (WikiImage) o;

		if (name != null ? !name.equals(wikiImage.name) : wikiImage.name != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		return name != null ? name.hashCode() : 0;
	}

	@Override
	public String toString() {
		return "WikiImage{" +
				"name='" + name + '\'' +
				'}';
	}
}
