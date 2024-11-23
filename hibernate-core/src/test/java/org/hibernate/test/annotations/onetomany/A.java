/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.onetomany;

import java.util.ArrayList;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Cascade;

/**
 * @author Peter Kotula
 */
@Entity
public class A {
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Id
	Long id;
	

	@NotNull
	String name;
	
    @OneToMany( cascade = CascadeType.ALL)
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
	@OrderBy("name")
	java.util.List<B> bs = new ArrayList<B>();
	

	
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

	public java.util.List<B> getBs() {
		return bs;
	}

//	public void setBs(java.util.List<B> bs) {
//		this.bs = bs;
//	}

	@Override
	public String toString() {
		return "A [id=" + id + ", name=" + name + ", bs=" + bs + "]";
	}
}
