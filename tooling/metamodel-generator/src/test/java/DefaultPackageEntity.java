/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */


import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class DefaultPackageEntity {
	@Id
	private long id;
}


