package org.hibernate.orm.test.annotations.type.dynamicparameterized;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import org.hibernate.annotations.Type;

import java.util.Map;

/**
 * @author Yanming Zhou
 */
@Entity
@Access(AccessType.FIELD)
public class Entity3 extends AbstractEntity {

	@Type( MyGenericType.class )
	Map<String, String> attributes;

}
