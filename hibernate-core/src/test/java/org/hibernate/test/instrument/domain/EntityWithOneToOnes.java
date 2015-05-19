/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.instrument.domain;


/**
 * @author Gail Badner
 */
public class EntityWithOneToOnes {
	private Long id;
	private String name;
	private OneToOneNoProxy oneToOneNoProxy;
	private OneToOneProxy oneToOneProxy;

	public EntityWithOneToOnes() {
	}

	public EntityWithOneToOnes(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public OneToOneNoProxy getOneToOneNoProxy() {
		return oneToOneNoProxy;
	}

	public void setOneToOneNoProxy(OneToOneNoProxy oneToOneNoProxy) {
		this.oneToOneNoProxy = oneToOneNoProxy;
	}

	public OneToOneProxy getOneToOneProxy() {
		return oneToOneProxy;
	}

	public void setOneToOneProxy(OneToOneProxy oneToOneProxy) {
		this.oneToOneProxy = oneToOneProxy;
	}
}
