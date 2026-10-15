package org.hibernate.orm.test.boot.models.hbm.joinformula;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

/**
 * @author Steve Ebersole
 */
@Entity
@IdClass(SharedColumnAddressId.class)
public class SharedColumnAddress {
	@Id
	@Column(name = "personName")
	private String person;
	@Id
	@Column(name = "addressType")
	private String type;
	private String street;
}
