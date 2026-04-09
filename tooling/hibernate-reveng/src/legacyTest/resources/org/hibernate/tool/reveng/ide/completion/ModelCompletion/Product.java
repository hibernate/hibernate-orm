/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2004-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.ide.completion.ModelCompletion;

import java.math.BigDecimal;
import java.util.Set;

/**
 * @author leon
 */
public class Product {

    private Long id;

    private int version;

    private Double weight;

    private BigDecimal price;

    private Set<Store> stores;

    private ProductOwner owner;

	private Set<ProductOwner> otherOwners;

    public Product() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Set<Store> getStores() {
        return stores;
    }

    public void setStores(Set<Store> stores) {
        this.stores = stores;
    }

    public void setOwner(ProductOwner owner) {
        this.owner = owner;
    }

    public ProductOwner getOwner() {
        return owner;
    }
    
    public Set<ProductOwner> getOtherOwners() {
    	return otherOwners;
    }

    public void setOtherOwners(Set<ProductOwner> otherOwners) {
		this.otherOwners = otherOwners;
	}
 }
