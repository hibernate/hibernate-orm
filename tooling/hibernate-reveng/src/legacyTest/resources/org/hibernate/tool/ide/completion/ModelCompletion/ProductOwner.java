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

/**
 * @author leon
 */
public class ProductOwner {

    private String firstName;

    private String lastName;

    private ProductOwnerAddress address;
    
    private StoreCity account;
    
    public ProductOwner() {
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setAddress(ProductOwnerAddress address) {
        this.address = address;
    }

    public ProductOwnerAddress getAddress() {
        return address;
    }

    public StoreCity getAccount() {
        return account;
    }

    public void setAccount(StoreCity account) {
        this.account = account;
    }


}
