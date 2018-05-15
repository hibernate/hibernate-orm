/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.relational;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.boot.model.domain.NotYetResolvedException;
import org.hibernate.boot.model.domain.ValueMapping;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.RuntimeCreationHelper;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;

/**
 * Any ValueMapping which exports a foreign key
 *
 * @author Steve Ebersole
 */
public interface ForeignKeyExporter extends ValueMapping {
	/**
	 * The foreign key this value represents.
	 */
	default MappedForeignKey getForeignKey() throws NotYetResolvedException {
		throw new NotYetImplementedFor6Exception();
	}

	// FK info
	//	 	1) the FK instance (unique instance for both sides in runtime model)
	// 		2) directionality = implied by the nature of the association
	// 		3) inverse - need to know which side "wins" in terms of defining column prototype(s) (names, types, etc)
	// 		4) owning - which side writes value(s) to the column(s)

	// process
	//		1) non-inverse side - create the FK instance with
	//		2) inverse-side - need to delay resolution until the corresponding non-inverse side is resolved - use
	//			the FK from the corresponding non-inverse side.  Alternatively, either side could trigger
	//			creation of the FK instance and we'd simply delay creation of the columns until after.

	default org.hibernate.metamodel.model.relational.spi.ForeignKey generateRuntimeForeignKey(
			RuntimeModelCreationContext creationContext,
			ForeignKey bootFk) {
		assert getForeignKey() == bootFk;

		return RuntimeCreationHelper.generateForeignKey( creationContext, bootFk );
	}
}
