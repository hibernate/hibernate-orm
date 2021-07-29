package org.hibernate.jpamodelgen.test.collectionbasictype;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

/**
 * @author Thomas Heigl
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class Like<T extends Like.I1 & Like.I2> {

	@Id
	private Long id;

	public abstract Reference<T> getObject();

	interface I1 {
	}

	interface I2 {
	}

	public static class Reference<T> {
	}

}
