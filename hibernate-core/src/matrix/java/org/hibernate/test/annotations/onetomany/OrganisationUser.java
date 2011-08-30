//$Id$
package org.hibernate.test.annotations.onetomany;
import java.io.Serializable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@PrimaryKeyJoinColumn( name = "id_organisation_user" )
@Table( name = "ORGANISATION_USER" )
public class OrganisationUser extends Person implements Serializable {

	private String someText;
	private Organisation organisation;

	public OrganisationUser() {
	}

	public void setSomeText(String someText) {
		this.someText = someText;
	}

	@Column( name = "some_text", nullable=true,length=1024)
	public String getSomeText() {
		return someText;
	}

	public void setOrganisation(Organisation organisation) {
		this.organisation = organisation;
	}

	@ManyToOne( cascade = {CascadeType.PERSIST, CascadeType.MERGE} )
	@JoinColumn( name = "fk_id_organisation", nullable = false )
	public Organisation getOrganisation() {
		return organisation;
	}

}
