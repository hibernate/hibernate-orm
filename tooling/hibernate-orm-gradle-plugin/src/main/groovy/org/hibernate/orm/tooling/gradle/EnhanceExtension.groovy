/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.tooling.gradle

/**
 * Gradle DSL extension for configuring various Hibernate bytecode enhancement.  Registered
 * under "hibernate.enhance".
 *
 * @author Steve Ebersole
 */
class EnhanceExtension {
	def boolean enableLazyInitialization = false
	def boolean enableDirtyTracking = false
	def boolean enableAssociationManagement = false
	def boolean enableExtendedEnhancement = false

	boolean shouldApply() {
		return enableLazyInitialization || enableDirtyTracking || enableAssociationManagement || enableExtendedEnhancement;
	}
}
