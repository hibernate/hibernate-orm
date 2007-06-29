//$Id: FooComponent.java 4599 2004-09-26 05:18:27Z oneovthafew $
package org.hibernate.test.legacy;

import java.io.Serializable;
import java.util.Date;

public class FooComponent implements Serializable {
	
	int count;
	String name;
	Date[] importantDates;
	FooComponent subcomponent;
	Fee fee = new Fee();
	GlarchProxy glarch;
	private FooProxy parent;
	private Baz baz;
	
	public boolean equals(Object that) {
		FooComponent fc = (FooComponent) that;
		return count==fc.count;
	}
	
	public int hashCode() {
		return count;
	}
	
	public String toString() {
		String result = "FooComponent: " + name + "=" + count;
		result+="; dates=[";
		if ( importantDates!=null) {
			for ( int i=0; i<importantDates.length; i++ ) {
				result+=(i==0 ?"":", ") + importantDates[i];
			}
		}
		result+="]";
		if ( subcomponent!=null ) {
			result+= " (" + subcomponent + ")";
		}
		return result;
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
	
	private String getNull() {
		return null;
	}
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
	
	public FooProxy getParent() {
		return parent;
	}
	
	public void setParent(FooProxy parent) {
		//if (parent==null) throw new RuntimeException("null parent set");
		this.parent = parent;
	}
	
	public Baz getBaz() {
		return baz;
	}
	
	public void setBaz(Baz baz) {
		this.baz = baz;
	}
	
}







