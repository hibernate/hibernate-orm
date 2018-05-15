/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entities.components;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

/**
 * The {@link #nonAuditedComponent} is ignored in {@link #hashCode()}
 * and {@link #equals(Object)} since it's not audited.
 *
 * @author Kristoffer Lundberg (kristoffer at cambio dot se)
 */
@Embeddable
@Audited
public class Component3 {
	private String str1;

	@AttributeOverrides({
								@AttributeOverride(name = "key", column = @Column(name = "audComp_key")),
								@AttributeOverride(name = "value", column = @Column(name = "audComp_value")),
								@AttributeOverride(name = "description", column = @Column(name = "audComp_description"))
						})
	private Component4 auditedComponent;

	@NotAudited
	@AttributeOverrides({
								@AttributeOverride(name = "key", column = @Column(name = "notAudComp_key")),
								@AttributeOverride(name = "value", column = @Column(name = "notAudComp_value")),
								@AttributeOverride(name = "description",
												   column = @Column(name = "notAudComp_description"))
						})
	private Component4 nonAuditedComponent;

	public Component3() {
	}

	public Component3(String str1, Component4 auditedComponent, Component4 nonAuditedComponent) {
		this.str1 = str1;
		this.auditedComponent = auditedComponent;
		this.nonAuditedComponent = nonAuditedComponent;
	}

	public String getStr1() {
		return str1;
	}

	public void setStr1(String str1) {
		this.str1 = str1;
	}

	public Component4 getAuditedComponent() {
		return auditedComponent;
	}

	public void setAuditedComponent(Component4 auditedComponent) {
		this.auditedComponent = auditedComponent;
	}

	public Component4 getNonAuditedComponent() {
		return nonAuditedComponent;
	}

	public void setNonAuditedComponent(Component4 nonAuditedComponent) {
		this.nonAuditedComponent = nonAuditedComponent;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((auditedComponent == null) ? 0 : auditedComponent.hashCode());
		result = prime * result + ((str1 == null) ? 0 : str1.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof Component3) ) {
			return false;
		}

		Component3 other = (Component3) obj;

		if ( auditedComponent != null ?
				!auditedComponent.equals( other.auditedComponent ) :
				other.auditedComponent != null ) {
			return false;
		}
		if ( str1 != null ? !str1.equals( other.str1 ) : other.str1 != null ) {
			return false;
		}

		return true;
	}

	@Override
	public String toString() {
		return "Component3[str1 = " + str1 + ", auditedComponent = "
				+ auditedComponent + ", nonAuditedComponent = "
				+ nonAuditedComponent + "]";
	}
}
