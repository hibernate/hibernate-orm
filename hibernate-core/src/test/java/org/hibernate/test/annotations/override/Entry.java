package org.hibernate.test.annotations.override;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.MappedSuperclass;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@MappedSuperclass
public abstract class Entry implements Serializable {
	@Id
	@GeneratedValue
	private Long id;

	@ElementCollection(fetch = FetchType.EAGER)
	@JoinTable(name = "TAGS", joinColumns = @JoinColumn(name = "ID"))
	@Column(name = "KEYWORD")
	@Fetch(FetchMode.JOIN)
	private Set<String> tags = new HashSet<String>();

	@Override
	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof Entry ) ) return false;

		Entry entry = (Entry) o;

		if ( id != null ? !id.equals( entry.id ) : entry.id != null ) return false;
		if ( tags != null ? !tags.equals( entry.tags ) : entry.tags != null ) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + ( tags != null ? tags.hashCode() : 0 );
		return result;
	}

	@Override
	public String toString() {
		return "Entry(id = " + id + ", tags = " + tags + ")";
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Set<String> getTags() {
		return tags;
	}

	public void setTags(Set<String> tags) {
		this.tags = tags;
	}
}
