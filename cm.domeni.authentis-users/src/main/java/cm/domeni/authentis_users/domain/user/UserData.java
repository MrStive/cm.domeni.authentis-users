package cm.domeni.authentis_users.domain.user;

import lombok.Builder;

@Builder
public record UserData(
    UserId id,
    UserName userName,
    Email email,
    Password password,
    FirstName firstName,
    LastName lastName,
    BirthDate birthDate,
    Address address) {}
