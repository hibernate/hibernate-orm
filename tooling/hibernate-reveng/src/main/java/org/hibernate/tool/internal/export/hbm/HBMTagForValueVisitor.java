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
package org.hibernate.tool.internal.export.hbm;

import org.hibernate.mapping.Any;
import org.hibernate.mapping.Array;
import org.hibernate.mapping.Bag;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.IdentifierBag;
import org.hibernate.mapping.List;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.Map;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PrimitiveArray;
import org.hibernate.mapping.Set;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.tool.internal.export.common.DefaultValueVisitor;

public class HBMTagForValueVisitor extends DefaultValueVisitor {

	public static final HBMTagForValueVisitor INSTANCE = new HBMTagForValueVisitor();
	
	protected HBMTagForValueVisitor() {
		super(true);
	}
	
	public Object accept(Bag bag) {
		return "bag";
	}

	public Object accept(IdentifierBag bag) {
		return "idbag";
	}

	public Object accept(List list) {
		return "list";
	}

	public Object accept(Map map) {
		return "map";
	}

	public Object accept(OneToMany many) {
		return "one-to-many";
	}

	public Object accept(Set set) {
		return "set";
	}

	public Object accept(Any any) {
		return "any";
	}

	public Object accept(SimpleValue value) {		
		return "property";
	}

	public Object accept(BasicValue value) {		
		return "property";
	}

	public Object accept(PrimitiveArray primitiveArray) {
		return "primitive-array";
	}

	public Object accept(Array list) {
		return "array";
	}

	public Object accept(DependantValue value) {
		throw new IllegalArgumentException("No tag for " + value);
	}

	public Object accept(Component component) {
		return component.isDynamic()?"dynamic-component":"component";
	}

	public Object accept(ManyToOne mto) {
		return "many-to-one";
	}

	public Object accept(OneToOne oto) {
		return "one-to-one";
	}
}
