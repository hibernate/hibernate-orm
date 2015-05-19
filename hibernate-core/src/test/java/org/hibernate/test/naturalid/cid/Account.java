/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.naturalid.cid;

/**
 * @author Donnchadh O Donnabhain
 */
public class Account {
    private AccountId accountId;
    private String shortCode;
    
    protected Account() {
    }
    
    public Account(AccountId accountId, String shortCode) {
        this.accountId = accountId;
        this.shortCode = shortCode;
    }
    public String getShortCode() {
        return shortCode;
    }
    public AccountId getAccountId() {
        return accountId;
    }
}
