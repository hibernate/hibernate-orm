package org.hibernate.orm.test.jpa.metamodel;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity
@Table(name = "translation_tbl")
public class Translation {
	@Id
	Integer id;
	String title;
	String text;
}
