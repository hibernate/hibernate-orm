/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.unionsubclass.alias;


/**
 * 
 * @author Strong Liu <stliu@redhat.com>
 */
public class CarBuyer extends Customer {
    private String sellerName;
    private String pid;
    private Seller seller;

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName( String sellerName ) {
        this.sellerName = sellerName;
    }

    public String getPid() {
        return pid;
    }

    public void setPid( String pid ) {
        this.pid = pid;
    }

    public Seller getSeller() {
        return seller;
    }

    public void setSeller( Seller seller ) {
        this.seller = seller;
    }

}
