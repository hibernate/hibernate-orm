/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.annotations.collectionelement;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Table;

/**
 * @author Steve Ebersole
 * @author Brett Meyer
 */
@Entity
// HHH-7732 -- "EntityWithAnElementCollection" is too long for Oracle.
@Table( name = "EWAEC" )
public class EntityWithAnElementCollection {
	private Long id;
	private Set<String> someStrings = new HashSet<String>();

	@Id
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ElementCollection
	// HHH-7732 -- "EntityWithAnElementCollection_someStrings" is too long for Oracle.
	@CollectionTable(
			name = "SomeStrings", 
			joinColumns = @JoinColumn( name = "EWAEC_ID") )
	public Set<String> getSomeStrings() {
		return someStrings;
	}

	public void setSomeStrings(Set<String> someStrings) {
		this.someStrings = someStrings;
	}
}
