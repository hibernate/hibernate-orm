package org.hibernate.processor.test.mappedsuperclass.typedmappedsuperclass;

import jakarta.persistence.Entity;

@Entity
public class AttachmentGroupPostInTopic extends AttachmentGroupPost<UserRole, AttachmentGroupInTopic> {
}
