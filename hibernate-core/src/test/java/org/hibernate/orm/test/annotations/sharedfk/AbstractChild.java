package org.hibernate.orm.test.annotations.sharedfk;

import jakarta.persistence.*;
import org.hibernate.annotations.DiscriminatorFormula;

import static jakarta.persistence.DiscriminatorType.INTEGER;

@Entity
@Table(name = " INHERITANCE_TAB")
//@DiscriminatorColumn(name = "DISC")
@DiscriminatorFormula(discriminatorType = INTEGER,
		value = "case when value1 is not null then 1 when value2 is not null then 2 end")
public abstract class AbstractChild {
	@Id
	@GeneratedValue
	@Column(name = "ID")
	Integer id;
}	
