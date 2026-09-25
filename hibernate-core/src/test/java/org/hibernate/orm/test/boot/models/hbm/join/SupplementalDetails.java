package org.hibernate.orm.test.boot.models.hbm.join;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Steve Ebersole
 */
@Entity
public class SupplementalDetails {
	@Id
	private Integer id;
	@Basic
	private String name;
}
