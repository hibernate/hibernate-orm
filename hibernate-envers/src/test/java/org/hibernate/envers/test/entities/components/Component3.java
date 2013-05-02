/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
