/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

/**
 * Describes in-line view source information.  Generally, either {@link org.hibernate.annotations.Subselect}
 * or {@code <subselect/>}
 *
 * @author Steve Ebersole
 */
public interface InLineViewSource extends TableSpecificationSource {
	/**
	 * Obtain the {@code SQL SELECT} statement to use.  Cannot be null!
	 *
	 * @return The {@code SQL SELECT} statement
	 */
	public String getSelectStatement();

	public String getLogicalName();
}
