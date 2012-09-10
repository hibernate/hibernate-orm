package org.hibernate.test.annotations.lob;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

/**
 * @author Janario Oliveira
 */
@Entity
@TypeDefs({ @TypeDef(typeClass = ImplicitSerializableType.class, defaultForType = ImplicitSerializable.class) })
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
