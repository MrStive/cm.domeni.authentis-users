package cm.domeni.authentis_users.service.mapper;

import cm.domeni.authentis_users.domain.user.*;
import cm.domeni.authentis_users.dto.AddressDTO;
import cm.domeni.authentis_users.dto.CreateUser;
import cm.domeni.authentis_users.dto.UserDTO;
import java.time.LocalDate;
import java.util.Optional;
import org.mapstruct.BeanMapping;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface UserMapper {
  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "userName")
  @Mapping(target = "email")
  @Mapping(target = "password")
  @Mapping(target = "firstName")
  @Mapping(target = "lastName")
  @Mapping(target = "birthDate")
  @Mapping(target = "address")
  UserData map(CreateUser createUser);

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "id", source = "id.value")
  @Mapping(target = "userName", source = "userName.value")
  @Mapping(target = "email", source = "email.value")
  @Mapping(target = "password", source = "password.value")
  @Mapping(target = "firstName", source = "firstName.value")
  @Mapping(target = "lastName", source = "lastName.value")
  @Mapping(target = "birthDate", source = "birthDate.value")
  @Mapping(target = "address")
  UserDTO map(User user);

  default UserName mapUserName(String value) {
    return Optional.ofNullable(value).map(UserName::new).orElse(null);
  }

  default Email mapEmail(String value) {
    return Optional.ofNullable(value).map(Email::new).orElse(null);
  }

  default Password mapPassword(String value) {
    return Optional.ofNullable(value).map(Password::new).orElse(null);
  }

  default FirstName mapFirstName(String value) {
    return Optional.ofNullable(value).map(FirstName::new).orElse(null);
  }

  default LastName mapLastName(String value) {
    return Optional.ofNullable(value).map(LastName::new).orElse(null);
  }

  default BirthDate mapBirthDate(LocalDate value) {
    return Optional.ofNullable(value).map(BirthDate::new).orElse(null);
  }

  default Address mapAddress(AddressDTO value) {
    return Optional.ofNullable(value)
        .map(addressDTO -> new Address(addressDTO.getCity()))
        .orElse(null);
  }

  default String map(UserName value) {
    return Optional.ofNullable(value).map(UserName::getValue).orElse(null);
  }

  default String map(Email value) {
    return Optional.ofNullable(value).map(Email::getValue).orElse(null);
  }

  default String map(Password value) {
    return Optional.ofNullable(value).map(Password::getValue).orElse(null);
  }

  default String map(FirstName value) {
    return Optional.ofNullable(value).map(FirstName::getValue).orElse(null);
  }

  default String map(LastName value) {
    return Optional.ofNullable(value).map(LastName::getValue).orElse(null);
  }

  default AddressDTO map(Address value) {
    return Optional.ofNullable(value)
        .map(
            address -> {
              var addressDTO = new AddressDTO();
              addressDTO.setCity(address.getCity());
              return addressDTO;
            })
        .orElse(null);
  }
}
