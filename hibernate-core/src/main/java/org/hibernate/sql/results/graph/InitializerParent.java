/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph;


import org.hibernate.Hibernate;

/**
 * Provides access to information about the owner/parent of a fetch
 * in relation to the current "row" being processed.
 *
 * @author Steve Ebersole
 */
public interface InitializerParent<Data extends InitializerData> extends Initializer<Data> {
	default Object getResolvedInstanceNoProxy(Data data){
		return Hibernate.unproxy( getResolvedInstance( data ) );
	}
}
