package org.hibernate.processor.test.mappedsuperclass.typedmappedsuperclass;

import jakarta.persistence.Entity;

@Entity
public class AttachmentGroupInTopic
		extends AttachmentGroup<AttachmentGroupInTopic, AttachmentGroupPostInTopic, UserRole> {
	public AttachmentGroupInTopic() {
	}
}
