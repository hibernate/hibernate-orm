// $Id:$
/*
* JBoss, Home of Professional Open Source
* Copyright 2009, Red Hat, Inc. and/or its affiliates, and individual contributors
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.hibernate.test.annotations.derivedidentities;

import java.io.Serializable;

/**
 * @author Hardy Ferentschik
 */
public class EmployerId implements Serializable {
	String name; // matches name of @Id attribute
	long employee; // matches name of @Id attribute and type of Employee PK

	public EmployerId() {
	}

	public EmployerId(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setEmployee(long employee) {
		this.employee = employee;
	}
}