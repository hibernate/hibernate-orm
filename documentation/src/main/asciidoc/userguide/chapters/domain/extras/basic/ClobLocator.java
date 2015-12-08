@Entity
public class Product {
	...
	@Lob
	@Basic
	public Clob description;
	...
}