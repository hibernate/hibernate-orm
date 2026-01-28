/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.hbm2x.hbm2hbmxml.CompositeElementTest;

import java.util.List;
import java.util.Map;

public interface GlarchProxy {

	int getVersion();
	int getDerivedVersion();
	void setVersion(int version);

	String getName();
	void setName(String name);

	GlarchProxy getNext();
	void setNext(GlarchProxy next);

	short getOrder();
	void setOrder(short order);

	List<Object> getStrings();
	void setStrings(List<Object> strings);

	Map<Object, Object> getDynaBean();
	void setDynaBean(Map<Object, Object> bean);

	Map<Object, Object> getStringSets();
	void setStringSets(Map<Object, Object> stringSets);

	List<Object> getFooComponents();
	void setFooComponents(List<Object> fooComponents);

	GlarchProxy[] getProxyArray();
	void setProxyArray(GlarchProxy[] proxyArray);

	Object getAny();
	void setAny(Object any);
}
