/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.hbm2x.hbm2hbmxml.ListArrayTest;

import java.io.ObjectStreamClass;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

public class FooComponent implements Serializable {

	@Serial
	private static final long serialVersionUID =
			ObjectStreamClass.lookup(FooComponent.class).getSerialVersionUID();

	int count;
	String name;
	Date[] importantDates;
	FooComponent subcomponent;
	Fee fee = new Fee();
	GlarchProxy glarch;

	public boolean equals(Object that) {
		if (that instanceof FooComponent fc) {
			return count == fc.count;
		} else {
			return false;
		}
	}

	public int hashCode() {
		return count;
	}

	public String toString() {
		StringBuilder result = new StringBuilder("FooComponent: " + name + "=" + count);
		result.append("; dates=[");
		if ( importantDates!=null) {
			for ( int i=0; i<importantDates.length; i++ ) {
				result.append(i == 0 ? "" : ", ").append(importantDates[i]);
			}
		}
		result.append("]");
		if ( subcomponent!=null ) {
			result.append(" (").append(subcomponent).append(")");
		}
		return result.toString();
	}

	public FooComponent() {}

	FooComponent(String name, int count, Date[] dates, FooComponent subcomponent) {
		this.name = name;
		this.count = count;
		this.importantDates = dates;
		this.subcomponent = subcomponent;
	}

	FooComponent(String name, int count, Date[] dates, FooComponent subcomponent, Fee fee) {
		this.name = name;
		this.count = count;
		this.importantDates = dates;
		this.subcomponent = subcomponent;
		this.fee = fee;
	}

	public int getCount() {
		return count;
	}
	public void setCount(int count) {
		this.count = count;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public Date[] getImportantDates() {
		return importantDates;
	}
	public void setImportantDates(Date[] importantDates) {
		this.importantDates = importantDates;
	}

	public FooComponent getSubcomponent() {
		return subcomponent;
	}
	public void setSubcomponent(FooComponent subcomponent) {
		this.subcomponent = subcomponent;
	}

	@SuppressWarnings("unused")
	private String getNull() {
		return null;
	}
	@SuppressWarnings("unused")
	private void setNull(String str) throws Exception {
		if (str!=null) throw new Exception("null component property");
	}
	public Fee getFee() {
		return fee;
	}

	public void setFee(Fee fee) {
		this.fee = fee;
	}

	public GlarchProxy getGlarch() {
		return glarch;
	}

	public void setGlarch(GlarchProxy glarch) {
		this.glarch = glarch;
	}

}
