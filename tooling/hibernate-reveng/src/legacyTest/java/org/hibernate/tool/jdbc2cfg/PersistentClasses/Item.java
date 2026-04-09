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
package org.hibernate.tool.jdbc2cfg.PersistentClasses;
public class Item {

    Integer childId;
    
    Orders order;
    Orders relatedorderId;
    String name;
    
    /**
     * @return Returns the id.
     */
    public Integer getChildId() {
        return childId;
    }
    /**
     * @param id The id to set.
     */
    public void setChildId(Integer id) {
        this.childId = id;
    }
    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }
    /**
     * @param name The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }
    /**
     * @return Returns the order.
     */
    public Orders getOrderId() {
        return order;
    }
    /**
     * @param order The order to set.
     */
    public void setOrderId(Orders order) {        
        this.order = order;
    }
    /**
     * @return Returns the order.
     */
    public Orders getOrdersByOrderId() {
        return order;
    }
    /**
     * @param order The order to set.
     */
    public void setOrdersByOrderId(Orders order) {
        this.order = order;
    }
    /**
     * @return Returns the relatedorderId.
     */
    public Orders getOrdersByRelatedOrderId() {
        return relatedorderId;
    }
    /**
     * @param relatedorderId The relatedorderId to set.
     */
    public void setOrdersByRelatedOrderId(Orders relatedorderId) {
        this.relatedorderId = relatedorderId;
    }
}
