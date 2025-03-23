/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.metamodel;

import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;

@Entity
public class ProductEntity {

	@Id
	private Long pk;

	@ElementCollection(targetClass = LocalizedValue.class)
	@CollectionTable(
		name = ( "product_name_lv" ),
		joinColumns = {
			@JoinColumn(
				name = ( "product_pk" )
			)
		},
		indexes = {
			@Index(
				name = ( "idx_product_name_lv" ),
				columnList = ( "product_pk" )
			)
		},
		foreignKey =
			@ForeignKey(
				name = ( "fk_product_name_lv" )
			)
	)
	@MapKeyColumn(name = "locale")
	private Map<String, ILocalizable> description = new HashMap<>();

	public Long getPk() {
		return pk;
	}

	public void setPk(Long pk) {
		this.pk = pk;
	}

	public Map<String, ILocalizable> getDescription() {
		return description;
	}

	public void setDescription(Map<String, ILocalizable> description) {
		this.description = description;
	}
}
