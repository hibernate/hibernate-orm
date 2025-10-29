/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2010-2025 Red Hat, Inc.
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
package org.hibernate.tool.reveng.internal.export.hbm;

import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.PersistentClassVisitor;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.UnionSubclass;

/**
 * @author max
 *
 */
public class HBMTagForPersistentClassVisitor implements PersistentClassVisitor {

	public static final PersistentClassVisitor INSTANCE = new HBMTagForPersistentClassVisitor();
	
	protected HBMTagForPersistentClassVisitor() {
		
	}

	public Object accept(RootClass class1) {
		return "class";
	}

	public Object accept(UnionSubclass subclass) {
		return "union-subclass";
	}

	public Object accept(SingleTableSubclass subclass) {
		return "subclass";
	}

	public Object accept(JoinedSubclass subclass) {
		return "joined-subclass";
	}

	public Object accept(Subclass subclass) {
		return "subclass";
	}

	

}
