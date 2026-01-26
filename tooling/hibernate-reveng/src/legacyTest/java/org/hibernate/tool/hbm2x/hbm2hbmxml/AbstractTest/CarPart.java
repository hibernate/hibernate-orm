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
package org.hibernate.tool.hbm2x.hbm2hbmxml.AbstractTest;

import java.io.ObjectStreamClass;
import java.io.Serial;

/**
 * @author Paco Hern�ndez
 */
public abstract class CarPart implements java.io.Serializable {

	@Serial
    private static final long serialVersionUID =
			ObjectStreamClass.lookup(CarPart.class).getSerialVersionUID();
		
	private long id;
	private String partName;

	/**
	 * @return Returns the id.
	 */
	public long getId() {
		return id;
	}
	/**
	 * @param id The id to set.
	 */
	public void setId(long id) {
		this.id = id;
	}
	/**
	 * @return Returns the typeName.
	 */
	public String getPartName() {
		return partName;
	}
	/**
	 * @param typeName The typeName to set.
	 */
	public void setPartName(String typeName) {
		this.partName = typeName;
	}
}
