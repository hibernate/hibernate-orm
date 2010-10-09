//$Id: BarProxy.java 4599 2004-09-26 05:18:27Z oneovthafew $
package org.hibernate.test.legacy;

public interface BarProxy extends AbstractProxy {
	public void setBaz(Baz arg0);
	public Baz getBaz();
	public void setBarComponent(FooComponent arg0);
	public FooComponent getBarComponent();
	//public void setBarString(String arg0);
	public String getBarString();
	public Object getObject();
	public void setObject(Object o);
}





