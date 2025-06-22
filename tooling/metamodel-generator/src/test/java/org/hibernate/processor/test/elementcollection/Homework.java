/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.elementcollection;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Bogdan Știrbăț
 */
@Entity
public class Homework {

	private long id;
	private List<String> paths;

	@Id
	public long getId() {
		return id;
	}

	@ElementCollection
	public List<String> getPaths() {
		return paths;
	}

	public Set<String> getPaths(String startPath) {
		TreeSet<String> result = new TreeSet<>();

		if ( paths == null ) {
			return result;
		}

		for ( String path : paths ) {
			if ( path.startsWith( startPath ) ) {
				result.add( path );
			}
		}
		return result;
	}

	public void setPaths(List<String> paths) {
		this.paths = paths;
	}

	public Homework setPaths(List<String> paths, boolean append) {
		if ( append ) {
			this.paths.addAll( paths );
		}
		else {
			this.paths = paths;
		}
		return this;
	}
}
