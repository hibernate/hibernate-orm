/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.quote;

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
