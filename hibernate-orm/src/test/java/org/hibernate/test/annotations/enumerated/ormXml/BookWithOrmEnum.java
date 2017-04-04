/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.enumerated.ormXml;

/**
 * @author Oliverio
 * @author Steve Ebersole
 */
public class BookWithOrmEnum {
	private Long id;
	private Binding bindingOrdinalEnum;
	private Binding bindingStringEnum;

	public Long getId() {
		return id;
	}

	public Binding getBindingOrdinalEnum() {
		return bindingOrdinalEnum;
	}

	public void setBindingOrdinalEnum(Binding bindingOrdinalEnum) {
		this.bindingOrdinalEnum = bindingOrdinalEnum;
	}

	public Binding getBindingStringEnum() {
		return bindingStringEnum;
	}

	public void setBindingStringEnum(Binding bindingStringEnum) {
		this.bindingStringEnum = bindingStringEnum;
	}
}
