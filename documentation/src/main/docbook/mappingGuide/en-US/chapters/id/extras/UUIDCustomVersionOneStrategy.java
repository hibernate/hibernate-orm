@Entity
public class MyEntity {
	@Id
	@GeneratedValue( generator="uuid" )
	@GenericGenerator(
			name="uuid",
			strategy="org.hibernate.id.UUIDGenerator",
			parameters = {
					@Parameter(
							name="uuid_gen_strategy_class",
							value="org.hibernate.id.uuid.CustomVersionOneStrategy"
					)
			}
	)
	public UUID id;
	...
}