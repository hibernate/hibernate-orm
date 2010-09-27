/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.hibernate.jpamodelgen.test.generics;

import java.util.Set;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name="ejb_parent")
public class Parent {

	@Embeddable
	public static class Relatives<T> {
		private Set<T> siblings;

		@OneToMany
		@JoinColumn(name="siblings_fk")
		public Set<T> getSiblings() {
			return siblings;
		}

		public void setSiblings(Set<T> siblings) {
			this.siblings = siblings;
		}
	}

	private Integer id;
	private String name;
	private Set<Child> children;
	private Relatives<Child> siblings;


	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@OneToMany(targetEntity = void.class)
	@JoinColumn(name="parent_fk", nullable = false)
	public Set<Child> getChildren() {
		return children;
	}

	public void setChildren(Set<Child> children) {
		this.children = children;
	}

	@Embedded
	public Relatives<Child> getSiblings() {
		return siblings;
	}

	public void setSiblings(Relatives<Child> siblings) {
		this.siblings = siblings;
	}
}
