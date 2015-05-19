/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.sortedcollection;

import java.util.SortedMap;
import java.util.SortedSet;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Sort;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class Printer {
	@Id
	@GeneratedValue
	private long id;

	@OneToMany
	@Sort
	private SortedSet<PrintJob> printQueue;

	@OneToMany
	@Sort
	private SortedMap<String, PrintJob> printedJobs;
}


