/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.generatedkeys.seqidentity;

import java.io.Serializable;
import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.OracleDialect;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "5" ),
				@Setting( name = AvailableSettings.USE_GET_GENERATED_KEYS, value = "true" )
		}
)
@DomainModel(
		annotatedClasses = {
				JoinedSequenceIdentityBatchTest.Resource.class,
				JoinedSequenceIdentityBatchTest.FolderResource.class
		}
)
@SessionFactory
@TestForIssue( jiraKey = "HHH-13365" )
@RequiresDialect( value = OracleDialect.class, version = 900 )
public class JoinedSequenceIdentityBatchTest {

	@Test
	public void testInsertAndUpdate(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					FolderResource folder = new FolderResource();
					folder.name = "PARENT";
					session.persist( folder );
				}
		);

		scope.inTransaction(
				session -> {
					List<FolderResource> folderResources = session.createQuery( "from FolderResource" ).getResultList();
					assertEquals( 1, folderResources.size() );
					final FolderResource folderResource = folderResources.get( 0 );
					assertNull( folderResource.description );
					folderResource.description = "A folder resource";
				}
		);

		scope.inTransaction(
				session -> {
					List<FolderResource> folderResources = session.createQuery( "from FolderResource" ).getResultList();
					assertEquals( 1, folderResources.size() );
					final FolderResource folderResource = folderResources.get( 0 );
					assertEquals( "A folder resource", folderResource.description );
				}
		);
	}

	@Entity(name = "Resource")
	@Inheritance(strategy = InheritanceType.JOINED)
	@Table(name = "WORKSPACE_RESOURCE")
	@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
	public static class Resource {

		@Id
    		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_WORKSPACE_RESOURCE")
    		@GenericGenerator(name = "SEQ_WORKSPACE_RESOURCE", strategy = "sequence-identity",
					parameters = {@Parameter(name = "sequence", value = "SEQ_WORKSPACE_RESOURCE")})
		@Column(name = "ID_WORKSPACE_RESOURCE", nullable = false, precision = 18)
		private Long id;

		@Column(name = "NAME", nullable = false, length = 256)
		protected String name;

		@Column(name = "RESOURCE_TYPE", nullable = false, length = 20)
		protected String type;
	}

	@Entity(name = "FolderResource")
	@Table(name = "WORKSPACE_RESOURCE_FOLDER")
	@PrimaryKeyJoinColumn(name = "ID_WORKSPACE_RESOURCE_FOLDER")
	@DiscriminatorValue("FOLDER")
	public static class FolderResource extends Resource implements Serializable {
		private String description;

		public FolderResource()
		{
			super();
			type = "FOLDER";
		}
	}
}
