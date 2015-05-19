/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.HibernateException;

/**
 * Defines the contract for handling of merge events generated from a session.
 *
 * @author Gavin King
 */
public interface MergeEventListener extends Serializable {

    /** 
     * Handle the given merge event.
     *
     * @param event The merge event to be handled.
     * @throws HibernateException
     */
	public void onMerge(MergeEvent event) throws HibernateException;

    /** 
     * Handle the given merge event.
     *
     * @param event The merge event to be handled.
     * @throws HibernateException
     */
	public void onMerge(MergeEvent event, Map copiedAlready) throws HibernateException;

}
