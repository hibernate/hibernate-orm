/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy.inlinedirtychecking;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jakarta.persistence.Cacheable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.NaturalId;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				ManyToOnePropertyAccessByFieldTest.User.class,
				ManyToOnePropertyAccessByFieldTest.Office.class,
				ManyToOnePropertyAccessByFieldTest.Client.class,
				ManyToOnePropertyAccessByFieldTest.Request.class,
				ManyToOnePropertyAccessByFieldTest.InternalRequest.class,
				ManyToOnePropertyAccessByFieldTest.Phone.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.FORMAT_SQL, value = "false" ),
				@Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
				@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false" ),
				@Setting( name = AvailableSettings.USE_QUERY_CACHE, value = "false" ),
		}
)
@SessionFactory
@BytecodeEnhanced
@CustomEnhancementContext({ DirtyCheckEnhancementContext.class, NoDirtyCheckEnhancementContext.class })
@JiraKey("HHH-13705")
public class ManyToOnePropertyAccessByFieldTest {

	private Long userId;
	private Long targetUserId;
	private Long officeId;

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Log log = new Log();
					log.setCreationDate( OffsetDateTime.now() );


					Office office = buildOffice( "The office", "And", Collections.emptySet() );

					session.persist( office );
					officeId = office.getId();

					User user = new User();
					user.setOffice( office );
					user.setClient( office.getClient() );
					user.setName( "Fab" );
					user.setLog( log );
					user.setEmail( "fab@hibernate.org" );

					session.persist( user );

					userId = user.getId();

					user = new User();
					user.setOffice( office );
					user.setClient( office.getClient() );
					user.setName( "And" );
					user.setLog( log );
					user.setEmail( "and@hibernate.org" );

					session.persist( user );
					targetUserId = user.getId();
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testPersist(SessionFactoryScope scope) {
		final Statistics stats = scope.getSessionFactory().getStatistics();
		stats.clear();

		InternalRequest internalRequest = new InternalRequest( 1L );
		scope.inTransaction(
				session -> {
					User user = session.find( User.class, userId );
					internalRequest.setUser( user );
					assertThat( stats.getPrepareStatementCount(), is( 1L ) );

					User targetUser = session.find( User.class, targetUserId );
					assertThat( stats.getPrepareStatementCount(), is( 2L ) );

					internalRequest.setTargetUser( targetUser );

					session.persist( internalRequest );

				}
		);
		assertThat( stats.getPrepareStatementCount(), is( 3L ) );
	}

	@Test
	public void testDelete(SessionFactoryScope scope) {
		Set<Phone> officePhones = new HashSet<>();
		officePhones.add( new Phone( 1L, "landline", "028-234-9876" ) );
		officePhones.add( new Phone( 2L, "mobile", "072-122-9876" ) );
		Office office = buildOffice( "second office", "Fab", officePhones );
		scope.inTransaction(
				session -> {
					session.persist( office );
				}
		);

		scope.inTransaction(
				session -> {
					Office result = session.find( Office.class, office.id );
					session.remove( result );
				}
		);


		scope.inTransaction(
				session -> {
					List<Office> offices = session.createQuery( "from Office" ).list();
					assertThat( offices.size(), is( 1 ) );
					assertThat( offices.get( 0 ).getId(), is( officeId ) );

					List<Phone> phones = session.createQuery( "from Phone" ).list();
					assertThat( phones.size(), is( 0 ) );
				}
		);
	}

	@Test
	public void testUpdate(SessionFactoryScope scope) {
		InternalRequest internalRequest = new InternalRequest( 1L );
		scope.inTransaction(
				session -> {
					User user = session.find( User.class, userId );
					internalRequest.setUser( user );

					User targetUser = session.find( User.class, targetUserId );

					internalRequest.setTargetUser( targetUser );

					session.persist( internalRequest );
				}
		);

		scope.inTransaction(
				session -> {
					InternalRequest result = session.find( InternalRequest.class, internalRequest.getId() );
					assertThat( result.getTargetUser().getId(), is( targetUserId ) );
					assertThat( result.getUser().getId(), is( userId ) );
					result.setUser( null );
				}
		);

		scope.inTransaction(
				session -> {
					InternalRequest result = session.find( InternalRequest.class, internalRequest.getId() );
					assertThat( result.getTargetUser().getId(), is( targetUserId ) );
					assertThat( result.getUser(), is( nullValue() ) );

					User user = session.find( User.class, userId );
					result.setTargetUser( user );

				}
		);

		scope.inTransaction(
				session -> {
					InternalRequest result = session.find( InternalRequest.class, internalRequest.getId() );
					assertThat( result.getTargetUser().getId(), is( userId ) );
					assertThat( result.getUser(), is( nullValue() ) );

					User user = session.find( User.class, userId );
					result.setUser( user );

					Set<Phone> officePhones = new HashSet<>();
					officePhones.add( new Phone( 1L, "landline", "028-234-9876" ) );
					officePhones.add( new Phone( 2L, "mobile", "072-122-9876" ) );
					Office office = buildOffice( "second office", "Fab", officePhones );


					session.persist( office );

					List<Office> offices = new ArrayList<>();
					offices.add( office );

					user.setOffices( offices );
				}
		);

		scope.inTransaction(
				session -> {
					InternalRequest result = session.find( InternalRequest.class, internalRequest.getId() );
					assertThat( result.getTargetUser().getId(), is( userId ) );
					User user = result.getUser();
					assertThat( user.getId(), is( userId ) );
					List<Office> offices = user.getOffices();
					assertThat( offices.size(), is( 1 ) );
					Office office = offices.get( 0 );
					assertThat( office.getPhones().size(), is( 2 ) );
				}
		);
	}

	private Office buildOffice(String officename, String clientName, Set<Phone> phones) {
		Log log = new Log();
		log.setCreationDate( OffsetDateTime.now() );

		Office office;
		office = new Office();
		Client client = new Client();
		client.setName( clientName );
		client.setLog( log );

		office.setName( officename );
		office.setActive( true );
		office.setDescription( officename );
		office.setManaged( true );
		office.setLog( log );
		office.setClient( client );
		office.setPhones( phones );

		return office;
	}

	@Embeddable
	public static class Log {
		@Column(name = "`creationDate`", nullable = false)
		private OffsetDateTime creationDate;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "`idCreator`")
		private User creator;

		public OffsetDateTime getCreationDate() {
			return creationDate;
		}

		public void setCreationDate(OffsetDateTime creationDate) {
			this.creationDate = creationDate;
		}


		public User getCreator() {
			return creator;
		}

		public void setCreator(User creator) {
			this.creator = creator;
		}
	}

	@Entity(name = "User")
	@Table(name = "`User`")
	public static class User {
		@Id
		@GeneratedValue
		private Long id;

		@Column(length = 120, nullable = false)
		private String name;

		@Column(length = 200, nullable = false, unique = true)
		private String email;

		private String hash;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "idOffice")
		private Office office;

		@OneToMany
		private List<Office> offices;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "`idClient`")
		private Client client;

		@Embedded
		private Log log = new Log();

		public Long getId() {
			return id;
		}

		public Office getOffice() {
			return office;
		}

		public void setOffice(Office office) {
			this.office = office;
		}

		public Log getLog() {
			return log;
		}

		public void setLog(Log log) {
			this.log = log;
		}

		public Client getClient() {
			return client;
		}

		public void setClient(Client client) {
			this.client = client;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		public String getHash() {
			return hash;
		}

		public void setHash(String hash) {
			this.hash = hash;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public List<Office> getOffices() {
			return offices;
		}

		public void setOffices(List<Office> offices) {
			this.offices = offices;
		}
	}

	@Entity(name = "Office")
	public static class Office {

		@Id
		@GeneratedValue
		private Long id;

		@Column(length = 50, nullable = false)
		private String name;

		private String description;

		@Column(nullable = false)
		private Boolean isActive = true;

		@Column(nullable = false)
		private Boolean isManaged = false;

		@ManyToOne(optional = false, cascade = CascadeType.ALL)
		@JoinColumn(name = "idClient")
		private Client client;

		@OneToMany(cascade = CascadeType.ALL)
		private Set<Phone> phones = new HashSet<>();

		@Embedded
		private Log log = new Log();

		public Long getId() {
			return id;
		}

		public Client getClient() {
			return client;
		}

		public void setClient(Client client) {
			this.client = client;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public Boolean getActive() {
			return isActive;
		}

		public void setActive(Boolean active) {
			isActive = active;
		}

		public Boolean getManaged() {
			return isManaged;
		}

		public void setManaged(Boolean managed) {
			isManaged = managed;
		}

		public Log getLog() {
			return log;
		}

		public void setLog(Log log) {
			this.log = log;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Set<Phone> getPhones() {
			return phones;
		}

		public void setPhones(Set<Phone> phones) {
			this.phones = phones;
		}
	}

	@Entity(name = "Phone")
	public static class Phone {
		@Id
		private Long id;

		private String type;

		@NaturalId
		@Column(name = "`number`")
		private String number;

		public Phone() {
		}

		public Phone(Long id, String type, String number) {
			this.id = id;
			this.type = type;
			this.number = number;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getNumber() {
			return number;
		}

		public void setNumber(String number) {
			this.number = number;
		}
	}

	@Entity(name = "Client")
	public static class Client {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@Embedded
		private Log log = new Log();

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Log getLog() {
			return log;
		}

		public void setLog(Log log) {
			this.log = log;
		}
	}

	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(discriminatorType = DiscriminatorType.STRING, name = "type", length = 30)
	@Cacheable
	@Entity(name = "Request")
	public static abstract class Request {

		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "idUser")
		private User user;

		@Column(name = "`creationDate`", nullable = false)
		private OffsetDateTime creationDate = OffsetDateTime.now();

		@Enumerated(EnumType.STRING)
		@Column(length = 30, nullable = false, name = "status")
		private StatusRequest status = StatusRequest.REQUESTED;

		Request() {
		}

		public Request(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public User getUser() {
			return user;
		}

		public void setUser(User userSolicitacao) {
			this.user = userSolicitacao;
		}

		public OffsetDateTime getCreationDate() {
			return creationDate;
		}

		public void setCreationDate(OffsetDateTime creationDate) {
			this.creationDate = creationDate;
		}

		public StatusRequest getStatus() {
			return status;
		}

		public void setStatus(StatusRequest status) {
			this.status = status;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "InternalRequest")
	@DiscriminatorValue(value = "INTERN")
	public static class InternalRequest extends Request {

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "`idTargetUser`")
		private User targetUser;

		InternalRequest() {
		}

		public InternalRequest(Long id) {
			super( id );
		}


		public User getTargetUser() {
			return targetUser;
		}

		public void setTargetUser(User targetUser) {
			this.targetUser = targetUser;
		}
	}

	public enum StatusRequest {

		REQUESTED( "requested" ), WAITING( "Feedback waiting" );

		private final String description;

		StatusRequest(final String description) {
			this.description = description;
		}

		public String getDescription() {
			return description;
		}
	}
}
