package org.hibernate.orm.test.bootstrap.binding.annotations.access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;

import jakarta.persistence.Access;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Access(AccessType.FIELD)
public class Furniture extends BaseFurniture {

}
