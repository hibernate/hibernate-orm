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
package org.hibernate.test.collection.set;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Gail Badner
 */
public class User {

	private String userName;
	private Set sessionAttributeNames = new HashSet();
	private Collection< String > someBag = new ArrayList< String >();
	private List< String > someList = new ArrayList< String >();
	private Map< String, String > someMap = new HashMap< String, String >();

	User() {
	}

	public User( String name ) {
		userName = name;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName( String userName ) {
		this.userName = userName;
	}

	public Set getSessionAttributeNames() {
		return sessionAttributeNames;
	}

	public void setSessionAttributeNames( Set sessionAttributeNames ) {
		this.sessionAttributeNames = sessionAttributeNames;
	}

	public Collection getSomeBag() {
		return someBag;
	}

	public void setSomeBag( Collection someBag ) {
		this.someBag = someBag;
	}

	public List getSomeList() {
		return someList;
	}

	public void setSomeList( List someList ) {
		this.someList = someList;
	}

	public Map getSomeMap() {
		return someMap;
	}

	public void setSomeMap( Map someMap ) {
		this.someMap = someMap;
	}
}
