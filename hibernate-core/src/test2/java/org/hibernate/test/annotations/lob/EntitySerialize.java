/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.lob;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

/**
 * @author Janario Oliveira
 */
@Entity
@TypeDef(typeClass = ImplicitSerializableType.class, defaultForType = ImplicitSerializable.class)
public class EntitySerialize {
	@Id
	@GeneratedValue
	long id;

	@Lob
	ExplicitSerializable explicitLob;
	
	@Type(type = "org.hibernate.test.annotations.lob.ExplicitSerializableType")
	ExplicitSerializable explicit;

	ImplicitSerializable implicit;

	@Type(type = "org.hibernate.test.annotations.lob.ExplicitSerializableType")
	ImplicitSerializable explicitOverridingImplicit;

	/**
	 * common in ExplicitSerializable and ImplicitSerializable to create same property in both
	 * This property will not persist it have a default value per type
	 * 
	 * @author Janario Oliveira
	 */
	public interface CommonSerializable {
		String getDefaultValue();

		void setDefaultValue(String defaultValue);
	}
}
