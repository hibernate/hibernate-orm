package org.hibernate.orm.test.envers.integration.manytoone.lazy;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.MappedSuperclass;

/**
 * @author Chris Cranford
 */
@MappedSuperclass
@Access(AccessType.FIELD)
public class Base {
}
