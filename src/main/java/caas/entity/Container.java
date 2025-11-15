package caas.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table()
@Data
public class Bucket {
	@Id
	String id;
}
