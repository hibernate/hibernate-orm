package org.hibernate.orm.test.annotations.onetoone;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Party {
	@Id
	String partyId;

	@OneToOne(cascade=CascadeType.ALL)
	@PrimaryKeyJoinColumn
	PartyAffiliate partyAffiliate;
}
