/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.test.quote;

import java.util.List;

/**
 * @author Brett Meyer
 */
public class DataPoint {
	private long id;
	
	private String fooProp;
	
	private DataPointEnum fooEnum;
	
	private List<DataPointEnum> fooEnumList;
	
	private List<AssociatedDataPoint> oneToMany;
	
	private List<AssociatedDataPoint> manyToMany;
	
	public static enum DataPointEnum {
		FOO1, FOO2, FOO3;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getFooProp() {
		return fooProp;
	}

	public void setFooProp(String fooProp) {
		this.fooProp = fooProp;
	}

	public DataPointEnum getFooEnum() {
		return fooEnum;
	}

	public void setFooEnum(DataPointEnum fooEnum) {
		this.fooEnum = fooEnum;
	}

	public List<DataPointEnum> getFooEnumList() {
		return fooEnumList;
	}

	public void setFooEnumList(List<DataPointEnum> fooEnumList) {
		this.fooEnumList = fooEnumList;
	}

	public List<AssociatedDataPoint> getOneToMany() {
		return oneToMany;
	}

	public void setOneToMany(List<AssociatedDataPoint> oneToMany) {
		this.oneToMany = oneToMany;
	}

	public List<AssociatedDataPoint> getManyToMany() {
		return manyToMany;
	}

	public void setManyToMany(List<AssociatedDataPoint> manyToMany) {
		this.manyToMany = manyToMany;
	}
}
