package org.hibernate.processor.test.inheritance.unmappedclassinhierarchy;

public class NormalExtendsEntity extends BaseEntity {
	public String doSomething(String s) {
		return ( s != null ? s : "empty" );
	}
}
