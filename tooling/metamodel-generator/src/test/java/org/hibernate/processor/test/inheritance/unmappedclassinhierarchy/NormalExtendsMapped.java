package org.hibernate.processor.test.inheritance.unmappedclassinhierarchy;

public class NormalExtendsMapped extends MappedBase {
	public String doSomething(String s) {
		return ( s != null ? s : "empty" );
	}
}
