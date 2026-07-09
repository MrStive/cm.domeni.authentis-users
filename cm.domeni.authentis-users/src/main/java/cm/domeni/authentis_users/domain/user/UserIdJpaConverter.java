package cm.domeni.authentis_users.domain.user;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class UserIdJpaConverter implements AttributeConverter<UserId, String> {

  @Override
  public String convertToDatabaseColumn(UserId attribute) {
    if (attribute == null) {
      return null;
    }
    return attribute.getValue();
  }

  @Override
  public UserId convertToEntityAttribute(String dbData) {
    if (dbData == null) {
      return null;
    }
    return new UserId(dbData);
  }
}
