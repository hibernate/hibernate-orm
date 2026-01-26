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
package org.hibernate.tool.hbm2x.hbm2hbmxml.IdBagTest;

import java.util.ArrayList;
import java.util.List;

public class User {
	private String name;
	private List<Group> groups = new ArrayList<Group>();
	
	User() {}
	
	public User(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
	

	void setName(String name) {
		this.name = name;
	}

	public List<Group> getGroups() {
		return groups;
	}
	
	void setGroups(List<Group> groups) {
		this.groups = groups;
	}
	
}
