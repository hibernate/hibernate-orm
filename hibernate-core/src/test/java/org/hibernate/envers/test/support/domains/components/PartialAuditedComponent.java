/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.components;

import java.util.Objects;

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
 * @author Chris Cranford
 */
@Embeddable
@Audited
public class PartialAuditedComponent {
	private String str1;

	@AttributeOverrides({
			@AttributeOverride(name = "key", column = @Column(name = "audComp_key")),
			@AttributeOverride(name = "value", column = @Column(name = "audComp_value")),
			@AttributeOverride(name = "description", column = @Column(name = "audComp_description"))
	})
	private PartialAuditedNestedComponent auditedComponent;

	@NotAudited
	@AttributeOverrides({
			@AttributeOverride(name = "key", column = @Column(name = "notAudComp_key")),
			@AttributeOverride(name = "value", column = @Column(name = "notAudComp_value")),
			@AttributeOverride(name = "description",
					column = @Column(name = "notAudComp_description"))
	})
	private PartialAuditedNestedComponent nonAuditedComponent;

	public PartialAuditedComponent() {
	}

	public PartialAuditedComponent(String str1, PartialAuditedNestedComponent auditedComponent, PartialAuditedNestedComponent nonAuditedComponent) {
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

	public PartialAuditedNestedComponent getAuditedComponent() {
		return auditedComponent;
	}

	public void setAuditedComponent(PartialAuditedNestedComponent auditedComponent) {
		this.auditedComponent = auditedComponent;
	}

	public PartialAuditedNestedComponent getNonAuditedComponent() {
		return nonAuditedComponent;
	}

	public void setNonAuditedComponent(PartialAuditedNestedComponent nonAuditedComponent) {
		this.nonAuditedComponent = nonAuditedComponent;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		PartialAuditedComponent that = (PartialAuditedComponent) o;
		return Objects.equals( str1, that.str1 ) &&
				Objects.equals( auditedComponent, that.auditedComponent );
	}

	@Override
	public int hashCode() {
		return Objects.hash( str1, auditedComponent );
	}

	@Override
	public String toString() {
		return "PartialAuditedComponent{" +
				"str1='" + str1 + '\'' +
				", auditedComponent=" + auditedComponent +
				", nonAuditedComponent=" + nonAuditedComponent +
				'}';
	}
}
