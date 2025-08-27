/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.override;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

/**
 * @author Lukasz Antoniak
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
