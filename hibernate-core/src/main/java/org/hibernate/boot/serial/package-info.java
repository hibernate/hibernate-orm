/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/// API for storing and restoring a factory-ready ORM boot model.
///
/// [org.hibernate.boot.serial.MetadataSerialization] creates and reads an
/// opaque [org.hibernate.boot.serial.MetadataArchive]. Archive implementation
/// details and the restoration snapshots are internal contracts and are not
/// exposed by this package. Archives are trusted, version-exact build
/// artifacts, not untrusted interchange data.
package org.hibernate.boot.serial;
