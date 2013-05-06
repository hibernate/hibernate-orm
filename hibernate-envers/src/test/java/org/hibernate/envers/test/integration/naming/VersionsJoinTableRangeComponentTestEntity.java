package org.hibernate.envers.test.integration.naming;

import javax.persistence.AssociationOverride;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

import org.hibernate.envers.AuditOverride;
import org.hibernate.envers.AuditOverrides;
import org.hibernate.envers.test.entities.components.Component1;

/**
 * Test entity, containing two embedded components, which each contain a list of
 * {@link VersionsJoinTableRangeTestEntitySuperClass}-instances
 *
 * @author Erik-Berndt Scheper
 */
@Entity
@Table(name = "COMPONENT_TEST_ENTITY")
public class VersionsJoinTableRangeComponentTestEntity {
	@Id
	@GeneratedValue
	@Column(name = "ID")
	private Integer id;

	/**
	 * An embedded component, containing a list of
	 * {@link VersionsJoinTableRangeTestEntity}-instances.
	 */
	@Embedded
	@AssociationOverride(name = "range",
						 joinColumns = {
								 @JoinColumn(name = "VJTRCTE1_ID",
											 insertable = true,
											 updatable = false,
											 nullable = false)
						 })
	@org.hibernate.envers.Audited
	@AuditOverride(name = "range",
				   auditJoinTable = @org.hibernate.envers.AuditJoinTable(name = "JOIN_TABLE_COMPONENT_1_AUD",
																		 inverseJoinColumns = @JoinColumn(name = "VJTRTE_ID")))
	private VersionsJoinTableRangeComponent<VersionsJoinTableRangeTestEntity> component1 = new VersionsJoinTableRangeComponent<VersionsJoinTableRangeTestEntity>();

	/**
	 * An embedded component, containing a list of
	 * {@link VersionsJoinTableRangeTestAlternateEntity}-instances.
	 */
	@Embedded
	@AssociationOverride(name = "range",
						 joinColumns = {
								 @JoinColumn(name = "VJTRCTE2_ID",
											 insertable = true,
											 updatable = false,
											 nullable = false)
						 })
	@org.hibernate.envers.Audited
	@AuditOverrides(value = {
			@AuditOverride(name = "range",
						   auditJoinTable = @org.hibernate.envers.AuditJoinTable(name = "JOIN_TABLE_COMPONENT_2_AUD",
																				 inverseJoinColumns = @JoinColumn(name = "VJTRTAE_ID")))
	})
	private VersionsJoinTableRangeComponent<VersionsJoinTableRangeTestAlternateEntity> component2 = new VersionsJoinTableRangeComponent<VersionsJoinTableRangeTestAlternateEntity>();

	/**
	 * An embedded component, containing a list of NOT AUDITED
	 * {@link VersionsJoinTableRangeTestAlternateEntity}-instances.
	 */
	@Embedded
	@AttributeOverrides(value = {
			@AttributeOverride(name = "str1", column = @Column(name = "STR1")),
			@AttributeOverride(name = "str2", column = @Column(name = "STR2"))
	})
	@org.hibernate.envers.Audited
	@AuditOverrides(value = {@AuditOverride(name = "str2", isAudited = false)})
	private Component1 component3;

	/**
	 * Default constructor
	 */
	public VersionsJoinTableRangeComponentTestEntity() {
		super();
	}

	/**
	 * @return the id
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	protected void setId(Integer id) {
		this.id = id;
	}

	/**
	 * @return the component1
	 */
	public VersionsJoinTableRangeComponent<VersionsJoinTableRangeTestEntity> getComponent1() {
		return component1;
	}

	/**
	 * @param component1 the component1 to set
	 */
	public void setComponent1(
			VersionsJoinTableRangeComponent<VersionsJoinTableRangeTestEntity> component1) {
		this.component1 = component1;
	}

	/**
	 * @return the component2
	 */
	public VersionsJoinTableRangeComponent<VersionsJoinTableRangeTestAlternateEntity> getComponent2() {
		return component2;
	}

	/**
	 * @param component2 the component2 to set
	 */
	public void setComponent2(
			VersionsJoinTableRangeComponent<VersionsJoinTableRangeTestAlternateEntity> component2) {
		this.component2 = component2;
	}

	/**
	 * @return the component3
	 */
	public Component1 getComponent3() {
		return component3;
	}

	/**
	 * @param component3 the component3 to set
	 */
	public void setComponent3(Component1 component3) {
		this.component3 = component3;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((component1 == null) ? 0 : component1.hashCode());
		result = prime * result
				+ ((component2 == null) ? 0 : component2.hashCode());
		result = prime * result
				+ ((component3 == null) ? 0 : component3.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		VersionsJoinTableRangeComponentTestEntity other = (VersionsJoinTableRangeComponentTestEntity) obj;
		if ( component1 == null ) {
			if ( other.component1 != null ) {
				return false;
			}
		}
		else if ( !component1.equals( other.component1 ) ) {
			return false;
		}
		if ( component2 == null ) {
			if ( other.component2 != null ) {
				return false;
			}
		}
		else if ( !component2.equals( other.component2 ) ) {
			return false;
		}
		if ( component3 == null ) {
			if ( other.component3 != null ) {
				return false;
			}
		}
		else if ( !component3.equals( other.component3 ) ) {
			return false;
		}
		if ( id == null ) {
			if ( other.id != null ) {
				return false;
			}
		}
		else if ( !id.equals( other.id ) ) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder output = new StringBuilder();

		output.append( "VersionsJoinTableRangeComponentTestEntity {" );
		output.append( " id = \"" ).append( this.getId() ).append( "\"," );
		output.append( " component1 = \"" ).append( this.component1 )
				.append( "\", " );
		output.append( " component2 = \"" ).append( this.component2 ).append( "\"}" );
		output.append( " component3 = \"" ).append( this.component3 ).append( "\"}" );
		return output.toString();
	}

}
