/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.mappedsuperclass.typedmappedsuperclass;

import java.util.Set;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;

@MappedSuperclass
public abstract class AttachmentGroup<GroupType extends AttachmentGroup, PostType extends AttachmentGroupPost<UserRoleType, GroupType>, UserRoleType extends UserRole> {
	@OneToMany(mappedBy = "parentGroup")
	protected Set<PostType> posts;

	@Id
	long id;
}

