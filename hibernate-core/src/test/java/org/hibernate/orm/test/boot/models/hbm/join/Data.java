package org.hibernate.orm.test.boot.models.hbm.join;

import jakarta.persistence.Embeddable;

/**
 * @author Steve Ebersole
 */
@Embeddable
public class Data {
	private String first;
	private String second;
}
