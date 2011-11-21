package org.hibernate.test.criteria;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

/**
* @author Strong Liu <stliu@hibernate.org>
*/
@Entity
@Table(name = "roles")
public class Role extends VersionedRecord {
	@Id
	@Enumerated(EnumType.STRING)
	Code code;
}
