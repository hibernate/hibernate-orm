/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.criteria;

public class OrderLine {

  private int lineId = 0;
  
  private Order order;
  
  private String articleId;

  
  public int getLineId() {
    return lineId;
  }

  public Order getOrder() {
    return order;
  }  

  public String getArticleId() {
    return articleId;
  }

  public void setOrder(Order order) {
    this.order = order;
  }

  public void setArticleId(String articleId) {
    this.articleId = articleId;
  }
  
  public String toString() {
    return "[" + getLineId() + ":" + getArticleId() + "]";
  }
}
