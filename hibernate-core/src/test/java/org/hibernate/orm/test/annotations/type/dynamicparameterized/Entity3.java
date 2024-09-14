/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
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
