package org.hibernate.test.annotations.override;

import java.util.Map;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class PropertyRecord {
	@Id
	@GeneratedValue
	public Long id;

	@AttributeOverrides({
			@AttributeOverride(name = "key.street", column = @Column(name = "STREET_NAME")),
			@AttributeOverride(name = "value.size", column = @Column(name = "SQUARE_FEET")),
			@AttributeOverride(name = "value.tax", column = @Column(name = "ASSESSMENT"))
					})
	@ElementCollection
	public Map<Address, PropertyInfo> parcels;
}