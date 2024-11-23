/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.stat;

import javax.persistence.Basic;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.annotations.NaturalId;

@Entity
@Table( name = "t_acct" )
public class Account {
    @EmbeddedId
    private AccountId accountId;

    @Basic( optional = false )
    @NaturalId
    private String shortCode;
    
    protected Account() {
    }
    
    public Account(AccountId accountId, String shortCode) {
        this.accountId = accountId;
        this.shortCode = shortCode;
    }

    public AccountId getAccountId() {
        return accountId;
    }

    public String getShortCode() {
        return shortCode;
    }
}
