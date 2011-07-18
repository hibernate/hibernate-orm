/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.hibernate.jpamodelgen.test.inheritance.unmappedclassinhierarchy;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

@Entity
@Access(AccessType.FIELD)
public class BaseEntity {
	@Id
	@SequenceGenerator(name = "test1_id_gen", sequenceName = "test1_seq")
	@GeneratedValue(generator = "test1_id_gen", strategy = GenerationType.SEQUENCE)
	protected Integer id;

	protected String name;

	public BaseEntity() {
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}
}
