package org.hibernate.processor.test.mappedsuperclass.typedmappedsuperclass;

import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class Post<UserRoleType extends UserRole> {
}
