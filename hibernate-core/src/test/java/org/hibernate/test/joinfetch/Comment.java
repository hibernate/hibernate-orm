/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Comment.java 6797 2005-05-16 12:08:03Z oneovthafew $
package org.hibernate.test.joinfetch;
import java.util.Calendar;

/**
 * @author Gavin King
 */
public class Comment {
	
	private String text;
	private Item item;
	private Calendar timestamp;
	private Long id;

	public Item getItem() {
		return item;
	}
	public void setItem(Item item) {
		this.item = item;
	}
	public Calendar getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Calendar timestamp) {
		this.timestamp = timestamp;
	}
	
	Comment() {}
	public Comment(Item item, String comment) {
		this.text = comment;
		this.item = item;
		item.getComments().add(this);
		this.timestamp = Calendar.getInstance();
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}

}
