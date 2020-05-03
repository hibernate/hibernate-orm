/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.override;
import java.util.Map;
import java.util.Set;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

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

	@AttributeOverrides({
			@AttributeOverride(name = "key.street", column = @Column(name = "STREET_NAME")),
			@AttributeOverride(name = "value.size", column = @Column(name = "SQUARE_FEET")),
			@AttributeOverride(name = "value.tax", column = @Column(name = "ASSESSMENT"))
					})
	@ElementCollection
	@CollectionTable(name="LegacyParcels")
	public Map<Address, PropertyInfo> legacyParcels;

	@AttributeOverrides({
			@AttributeOverride(name = "size", column = @Column(name = "SQUARE_FEET")),
			@AttributeOverride(name = "tax", column = @Column(name = "ASSESSMENT"))
					})
	@ElementCollection
	public Set<PropertyInfo> unsortedParcels;

	@AttributeOverrides({
			@AttributeOverride(name = "size", column = @Column(name = "SQUARE_FEET")),
			@AttributeOverride(name = "tax", column = @Column(name = "ASSESSMENT"))
					})
	@ElementCollection
	public Set<PropertyInfo> legacyUnsortedParcels;
}
