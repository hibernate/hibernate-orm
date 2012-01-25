package org.hibernate.test.inheritancediscriminator;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;

import static javax.persistence.DiscriminatorType.INTEGER;
import static javax.persistence.GenerationType.IDENTITY;
import static javax.persistence.InheritanceType.SINGLE_TABLE;

/**
 * Created by Pawel Stawicki on 8/17/11 11:01 PM
 */

@Entity
@Inheritance(strategy = SINGLE_TABLE)
@DiscriminatorColumn(name = "CLASS_ID", discriminatorType = INTEGER)
public abstract class ParentEntity  {

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "ajdik")
	private Long id;

	public Long getId() {
		return id;
	}
}

