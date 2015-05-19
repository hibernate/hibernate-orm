/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

// $Id: Silly.java 9595 2006-03-10 18:14:21Z steve.ebersole@jboss.com $
package org.hibernate.test.connections;
import java.io.Serializable;

/**
 * Implementation of Silly.
 *
 * @author Steve Ebersole
 */
public class Silly implements Serializable {
	private Long id;
	private String name;
	private Other other;

	public Silly() {
	}

	public Silly(String name) {
		this.name = name;
	}

	public Silly(String name, Other other) {
		this.name = name;
		this.other = other;
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

	public Other getOther() {
		return other;
	}

	public void setOther(Other other) {
		this.other = other;
	}
}
