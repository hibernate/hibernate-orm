/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.naturalid.cid;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

import org.hibernate.annotations.NaturalId;

/**
 * @author Donnchadh O Donnabhain
 */
@Entity
public class A {
    @EmbeddedId
    private AId accountId;
    @NaturalId(mutable = false)
    private String shortCode;
    
    protected A() {
    }
    
    public A(AId accountId, String shortCode) {
        this.accountId = accountId;
        this.shortCode = shortCode;
    }
    public String getShortCode() {
        return shortCode;
    }
    public AId getAccountId() {
        return accountId;
    }
}
