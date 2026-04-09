/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2004-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.hbm2x.hbm2hbmxml.DynamicComponentTest;

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
		if (that instanceof FooComponent) {
			return count == ((FooComponent) that).count;
		}
		else return false;
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







