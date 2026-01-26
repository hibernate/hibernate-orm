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

package org.hibernate.tool.hbm2x.hbm2hbmxml.ListArrayTest;

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







