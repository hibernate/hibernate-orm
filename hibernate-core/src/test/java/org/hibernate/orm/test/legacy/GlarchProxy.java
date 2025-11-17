/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface GlarchProxy {

	public int getVersion();
	public int getDerivedVersion();
	public void setVersion(int version);

	public String getName();
	public void setName(String name);

	public GlarchProxy getNext();
	public void setNext(GlarchProxy next);

	public short getOrder();
	public void setOrder(short order);

	public List getStrings();
	public void setStrings(List strings);

	public Map getDynaBean();
	public void setDynaBean(Map bean);

	public Map getStringSets();
	public void setStringSets(Map stringSets);

	public List getFooComponents();
	public void setFooComponents(List fooComponents);

	public GlarchProxy[] getProxyArray();
	public void setProxyArray(GlarchProxy[] proxyArray);

	public Set getProxySet();
	public void setProxySet(Set proxySet);

	public Object getAny();
	public void setAny(Object any);
}
