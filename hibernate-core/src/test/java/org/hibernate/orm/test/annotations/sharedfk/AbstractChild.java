package org.hibernate.orm.test.annotations.sharedfk;

import jakarta.persistence.*;
import org.hibernate.annotations.DiscriminatorFormula;

@Entity
@Table(name = " INHERITANCE_TAB")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Access(AccessType.FIELD)
//@DiscriminatorColumn(name = "DISC")
@DiscriminatorFormula("case when value1 is not null then 1 when value2 is not null then 2 end")
public class AbstractChild {
	@Id
	@GeneratedValue
	@Column(name = "ID")
	Integer id;
}	
