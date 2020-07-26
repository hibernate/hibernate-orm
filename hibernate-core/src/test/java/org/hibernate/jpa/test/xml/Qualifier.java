/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.xml;

/**
 * @author <a href="mailto:stliu@hibernate.org">Strong Liu</a>
 */
public class Qualifier {
	private Long qualifierId;
	private String name;
	private String value;

	public Long getQualifierId() {
		return qualifierId;
	}

	public void setQualifierId(Long qualifierId) {
		this.qualifierId = qualifierId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
