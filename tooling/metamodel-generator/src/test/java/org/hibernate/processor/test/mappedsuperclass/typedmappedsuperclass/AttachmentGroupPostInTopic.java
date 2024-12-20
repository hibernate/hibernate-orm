/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.mappedsuperclass.typedmappedsuperclass;

import jakarta.persistence.Entity;

@Entity
public class AttachmentGroupPostInTopic extends AttachmentGroupPost<UserRole, AttachmentGroupInTopic> {
}
