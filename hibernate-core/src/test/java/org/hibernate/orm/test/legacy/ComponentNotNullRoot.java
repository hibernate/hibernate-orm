/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;

import java.util.List;

/**
 * Entity containing components for not-null testing
 *
 * @author Emmanuel Bernard
 */
public class ComponentNotNullRoot {

	private int id;
	private String test;
	private ComponentNotNull nullable;
	private ComponentNotNull supercomp;
	private List components;
	private List componentsImplicit;

	public int getId() {
		return id;
	}

	public ComponentNotNull getNullable() {
		return nullable;
	}

	public void setId(int i) {
		id = i;
	}

	public void setNullable(ComponentNotNull component) {
		nullable = component;
	}

	public static final class ContainerInnerClass {
		private Simple simple;
		private String name;
		private One one;
		private Many many;
		private int count;
		private ContainerInnerClass nested;
		private String nestedproperty;

		public void setSimple(Simple simple) {
			this.simple = simple;
		}

		public Simple getSimple() {
			return simple;
		}

		public String getName() {
			return name;
		}


		public void setName(String name) {
			this.name = name;
		}

		public String toString() {
			return name +  " = " + simple.getCount() +
			"/"  + ( one==null ? "nil" : one.getKey().toString() ) +
			"/"  + ( many==null ? "nil" : many.getKey().toString() );
		}

		public One getOne() {
			return one;
		}

		public void setOne(One one) {
			this.one = one;
		}

		public Many getMany() {
			return many;
		}

		public void setMany(Many many) {
			this.many = many;
		}

		public int getCount() {
			return count;
		}

		public void setCount(int count) {
			this.count = count;
		}

		public ContainerInnerClass getNested() {
			return nested;
		}

		public void setNested(ContainerInnerClass class1) {
			nested = class1;
		}

		public String getNestedproperty() {
			return nestedproperty;
		}

		public void setNestedproperty(String string) {
			nestedproperty = string;
		}

	}

	public List getComponents() {
		return components;
	}

	public void setComponents(List list) {
		components = list;
	}

	public List getComponentsImplicit() {
		return componentsImplicit;
	}

	public void setComponentsImplicit(List list) {
		componentsImplicit = list;
	}

	public ComponentNotNull getSupercomp() {
		return supercomp;
	}

	public void setSupercomp(ComponentNotNull component) {
		supercomp = component;
	}

	public String getTest() {
		return test;
	}

	public void setTest(String string) {
		test = string;
	}

}
