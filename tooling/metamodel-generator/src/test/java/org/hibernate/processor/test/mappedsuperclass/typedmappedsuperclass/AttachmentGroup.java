/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.mappedsuperclass.typedmappedsuperclass;

import java.util.Set;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;

@MappedSuperclass
public abstract class AttachmentGroup<GroupType extends AttachmentGroup, PostType extends AttachmentGroupPost<UserRoleType, GroupType>, UserRoleType extends UserRole> {
	@OneToMany(mappedBy = "parentGroup")
	protected Set<PostType> posts;

	@Id
	long id;
}
