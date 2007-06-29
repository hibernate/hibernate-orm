//$Id$
package org.hibernate.test.interfaceproxy;

import java.sql.Blob;
import java.util.Calendar;

/**
 * @author Gavin King
 */
public interface Document extends Item {
	/**
	 * @return Returns the content.
	 */
	public Blob getContent();

	/**
	 * @param content The content to set.
	 */
	public void setContent(Blob content);
	
	public Calendar getCreated();
	
	public Calendar getModified();
}