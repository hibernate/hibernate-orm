package org.hibernate.test.criteria;

import javax.persistence.MappedSuperclass;

/**
* @author Strong Liu <stliu@hibernate.org>
*/
@MappedSuperclass
abstract class VersionedRecord implements java.io.Serializable {
	Long recordVersion;
	Boolean isDeleted;
}
